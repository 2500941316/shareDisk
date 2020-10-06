package com.shu.hbase.service.impl;

import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import com.shu.hbase.tools.hdfspool.HdfsConnectionPool;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CrudMethods {
    private static Logger logger = LoggerFactory.getLogger(CrudMethods.class);

    //更新用户存储文件总大小
    public static boolean insertOrUpdateUser(Table userTable, String size, String uid, String type) {
        try {
            logger.info("开始查询用户的存储大小");
            //先获取当前用户存储的大小
            Get get = new Get(Bytes.toBytes(uid));
            get.addColumn(Bytes.toBytes(Static.USER_TABLE_CF), Bytes.toBytes(Static.USER_TABLE_SIZE));
            Result userRes = userTable.get(get);
            int KB = 1024;//定义GB的计算常量
            DecimalFormat df = new DecimalFormat("0.00");//格式化小数
            String resultSize = "";
            String newSize = null;
            resultSize = df.format(Integer.parseInt(size) / (float) KB);
            if (userRes.isEmpty()) {
                if (Double.parseDouble(resultSize) > 20 * 1024 * 1024) {
                    return false;
                }
                newSize = size;
            } else {
                String curSize = null;
                //先获得当前的存储大小
                for (Cell cell : userRes.rawCells()) {
                    if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.USER_TABLE_SIZE)) {
                        curSize = Bytes.toString(CellUtil.cloneValue(cell));
                        String GSize = df.format(Double.parseDouble(curSize) / (float) KB);
                        //如果是up类型则判断是否超限
                        if (type.equals("upload")) {
                            if (Double.parseDouble(GSize) + Double.parseDouble(resultSize) > 20 * 1024 * 1024)
                                return false;
                            newSize = Double.parseDouble(size) + Double.parseDouble(curSize) + "";
                        } else {
                            newSize = Double.parseDouble(curSize) - Double.parseDouble(size) + "";
                        }
                    }
                }
            }
            logger.info("磁盘校验完毕");
            Put put = new Put(Bytes.toBytes(uid));
            assert newSize != null;
            put.addColumn(Bytes.toBytes(Static.USER_TABLE_CF), Bytes.toBytes(Static.USER_TABLE_SIZE), Bytes.toBytes(newSize));
            userTable.put(put);
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }


    //将查询出来的cell封装为返回list的方法
    static FileInfoVO packageCells(Result result) {
        FileInfoVO fileInfoVO = new FileInfoVO();
        String fileId = null;
        for (Cell cell : result.rawCells()) {
            fileId = Bytes.toString(CellUtil.cloneRow(cell));
            if (fileId.length() == 0) {
                return null;
            }
            fileInfoVO.setFileId(fileId);

            fileInfoVO.setSharer(fileId.substring(0, 8));

            String re = Bytes.toString(CellUtil.cloneQualifier(cell));
            switch (re) {
                case "name":
                    fileInfoVO.setName(Bytes.toString(CellUtil.cloneValue(cell)));
                    break;
                case "path":
                    fileInfoVO.setPath(Bytes.toString(CellUtil.cloneValue(cell)));
                    break;
                case "size":
                    if (!Bytes.toString(CellUtil.cloneValue(cell)).equals("-")) {
                        fileInfoVO.setSize(Long.parseLong(Bytes.toString(CellUtil.cloneValue(cell))));
                    } else {
                        fileInfoVO.setSize(null);
                    }
                    break;
                case "dir":
                    fileInfoVO.setDir(Bytes.toString(CellUtil.cloneValue(cell)).equals("true"));
                    break;
                case "time":
                    fileInfoVO.setTime(Long.parseLong(Bytes.toString(CellUtil.cloneValue(cell))));
                    break;
                case "back":
                    fileInfoVO.setBack(Bytes.toString(CellUtil.cloneValue(cell)));
                    break;
                default:
                    break;
            }
        }
        //设置类型
        if (!fileInfoVO.isDir()) {
            //截取最后的后缀名
            String name = fileInfoVO.getName();
            String substring = "";
            if (name.lastIndexOf(".") != -1) {
                substring = name.substring(name.lastIndexOf("."));
            }
            if (!substring.isEmpty()) {
                fileInfoVO.setType(substring);
            } else {
                fileInfoVO.setType("common");
            }
        } else {
            fileInfoVO.setType("dir");
        }
        fileInfoVO.setNew(false);
        return fileInfoVO;
    }


    //根据fileId和用户来校验权限
    static boolean verifite(Table fileTable, String authId, String filedId, String gId) throws IOException {
        //通过fileTable查出该文件的权限信息
        //如果fileId为8位，则说明在查首页,只有本人能查到
        if ((filedId.length() == 8) || filedId.substring(0, 8).equals("00000000")) {
            return true;
        } else {

            Get get = new Get(Bytes.toBytes(filedId));
            get.setMaxVersions();
            get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth));
            Result result = fileTable.get(get);
            List<Cell> cells = result.listCells();
            String newAuthId = null;
            if (!gId.isEmpty()) {
                logger.info("gId不为空，开始组合新的AuthId进行验证");
                newAuthId = gId + authId;
            }
            for (Cell cell : cells) {
                if (Bytes.toString(CellUtil.cloneValue(cell)).equals(newAuthId) || Bytes.toString(CellUtil.cloneValue(cell)).equals(authId) || Bytes.toString(CellUtil.cloneValue(cell)).equals("公开")) {
                    return true;
                }
            }
        }
        return false;
    }

    //根据file表的文件id来查找，文件对应的物理路径
    public static String findUploadPath(String backId) {
        Connection hBaseConn = null;
        Table fileTable = null;
        String path = null;
        if (backId.length() == 8) {
            path = Static.BASEURL + backId;
        } else {
            try {
                hBaseConn = HbaseConnectionPool.getHbaseConnection();
                fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
                //根据gid查询每个组的文件
                Get get = new Get(Bytes.toBytes(backId));
                get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_PATH));
                Result result = fileTable.get(get);
                Cell cell = result.rawCells()[0];
                path = Bytes.toString(CellUtil.cloneValue(cell));

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    assert fileTable != null;
                    fileTable.close();
                    HbaseConnectionPool.releaseConnection(hBaseConn);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return path;
    }


    //将上传的文件名和新建的文件夹 checkTable方法中新建的文件插入files表中
    public static boolean insertToFiles(File localPath, String fileType, String hdfsPath, String backId, String uId, String fileId) {
        Connection hBaseConn = null;
        Table fileTable = null;
        if (!backId.substring(0, 8).equals(uId)) {
            return false;
        }
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));

            long l = System.currentTimeMillis();
            Put put = new Put(Bytes.toBytes(fileId));
            if (localPath != null) {
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_NAME), Bytes.toBytes(localPath.getName()));
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_SIZE), Bytes.toBytes(String.valueOf(localPath.length())));
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_ISDIR), Bytes.toBytes("false"));
            } else {
                String newPath = hdfsPath;
                newPath = newPath.substring(newPath.lastIndexOf("/") + 1);
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_NAME), Bytes.toBytes(newPath));
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_SIZE), Bytes.toBytes("-"));
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_ISDIR), Bytes.toBytes("true"));
            }
            //如果是首页的数据则back设为/+学号；如果不是首页的数据则back设为当前文件夹的id号
            put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_TYPE), Bytes.toBytes(fileType));
            put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_BACK), Bytes.toBytes(backId));
            put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_PATH), Bytes.toBytes(hdfsPath));

            //如果当前的backid不等于uid，说明在一个文件夹下面上传文件，则先查询文件夹的权限，然后等于该文件夹的权限
            if (!backId.equals(uId)) {
                //查询fileId是backId的文件夹的权限
                Get authGet = new Get(Bytes.toBytes(backId));
                authGet.setMaxVersions();
                authGet.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth));
                Result authResult = fileTable.get(authGet);
                List<String> authList = new ArrayList<>();
                if (!authResult.isEmpty()) {
                    for (Cell cell : authResult.rawCells()) {
                        if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.FILE_TABLE_Auth)) {
                            authList.add(Bytes.toString(CellUtil.cloneValue(cell)));
                        }
                    }
                }
                for (String auth : authList) {
                    put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth), ++l, Bytes.toBytes(auth));
                }
            } else {
                put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth), Bytes.toBytes(uId));
            }

            put.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_TIME), Bytes.toBytes(l + ""));
            fileTable.put(put);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HbaseConnectionPool.releaseConnection(hBaseConn);
        }
        return true;
    }


    //单个物理目录的删除
    public static boolean delete(String detSrc) {
        FileSystem fs = null;
        //true意思是递归删除，如果不为空也删除
        try {
            fs = HdfsConnectionPool.getHdfsConnection();
            fs.delete(new Path(detSrc), true);
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                HdfsConnectionPool.releaseConnection(fs);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            return true;
        }
    }


    //递归删除某个fileId下面的全部文件的方法
    public static void deleteFilesById(Table fileTable, List<Delete> deleteList, String fileId, String uId, List sizeList) throws IOException {
        Scan scan = new Scan();
        FilterList filterList = new FilterList();
        logger.info("开始查询所有backid是删除对象的文件，也要进行删除");
        SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                Static.FILE_TABLE_CF.getBytes(),
                Static.FILE_TABLE_BACK.getBytes(),
                CompareFilter.CompareOp.EQUAL,
                new BinaryComparator(Bytes.toBytes(fileId)));
        singleColumnValueFilter.setFilterIfMissing(true);
        filterList.addFilter(singleColumnValueFilter);

        scan.setFilter(filterList);
        ResultScanner scanner = fileTable.getScanner(scan);
        for (Result result : scanner) {
            for (Cell cell : result.rawCells()) {
                if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.FILE_TABLE_SIZE)) {
                    logger.info("检测扫描的文件是否是文件夹");
                    logger.info(Bytes.toString(CellUtil.cloneValue(cell)));
                    if (!Bytes.toString(CellUtil.cloneValue(cell)).equals("-") && !Bytes.toString(CellUtil.cloneValue(cell)).isEmpty())
                        logger.info("扫描的对象部署文件夹");
                        sizeList.add(Integer.parseInt(Bytes.toString(CellUtil.cloneValue(cell))));
                }
            }
            //获得子文件的fileid，放入deleteFnHbase方法中
            String newFile = Bytes.toString(result.getRow());
            deleteFnHbase(newFile, null, uId);
            Delete delete = new Delete(Bytes.toBytes(newFile));
            deleteList.add(delete);
            deleteFilesById(fileTable, deleteList, newFile, uId, sizeList);
        }
    }



    //删除一个group最外层文件夹的所有文件
    public static boolean deleteFnHbase(String fileId, String gId, String uId) {
        Connection hbaseConnection = null;
        Table indexTable = null;
        Table groupTable = null;
        Table fileTable = null;
        try {
            hbaseConnection = HbaseConnectionPool.getHbaseConnection();
            indexTable = hbaseConnection.getTable(TableName.valueOf(Static.INDEX_TABLE));
            groupTable = hbaseConnection.getTable(TableName.valueOf(Static.GROUP_TABLE));
            fileTable = hbaseConnection.getTable(TableName.valueOf(Static.FILE_TABLE));

            //检查index表中所有的组中有没有该路径，如果有则删除该版本
            Get get = new Get(Bytes.toBytes(uId));
            get.setMaxVersions();
            if (gId != null) {
                get.addColumn(Bytes.toBytes(Static.INDEX_TABLE_CF), Bytes.toBytes(gId)); //如果删除的是共享组的某个文件
            } else {
                get.addFamily(Bytes.toBytes(Static.INDEX_TABLE_CF));
            }

            Result result = indexTable.get(get);
            List<Delete> deleteList = new ArrayList<>();
            List<String> authIdList = new ArrayList<>();
            List<Delete> indexDeList = new ArrayList<>();
            List<Delete> groupDeList = new ArrayList<>();
            for (Cell cell : result.rawCells()) {
                if (Bytes.toString(CellUtil.cloneValue(cell)).length() != 0) {
                    //如果fileId存在index表的该格子中，则删除该fileId的版本
                    if (Bytes.toString(CellUtil.cloneValue(cell)).equals(fileId)) {
                        Delete indexDelete = new Delete(Bytes.toBytes(uId));
                        indexDelete.addColumn(Bytes.toBytes(Static.INDEX_TABLE_CF), CellUtil.cloneQualifier(cell), cell.getTimestamp());
                        indexDeList.add(indexDelete);

                        //获取group表中该组的所以成员和文件id：（要删除组的节奏）遍历结果，如果是成员的列则封装成auth，必须删除每一个取得的auth
                        Get groupGet = new Get(CellUtil.cloneQualifier(cell));
                        groupGet.setMaxVersions();
                        groupGet.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_fileId));
                        groupGet.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_MEMBER));
                        Result groupRes = groupTable.get(groupGet);
                        //遍历该组的成员和fileid，获得所有的有要删除的文件权限的gid+uid
                        for (Cell rawCell : groupRes.rawCells()) {
                            if (Bytes.toString(CellUtil.cloneValue(rawCell)).length() != 0) {
                                //如果列名等于组成员并且列值不等于uid的话，说明不是此文件的拥有者，则gid+uid组合成权限值，准备删除
                                if (Bytes.toString(CellUtil.cloneQualifier(rawCell)).equals(Static.GROUP_TABLE_MEMBER) && !Bytes.toString(CellUtil.cloneValue(rawCell)).equals(uId)) {
                                    authIdList.add(gId + Bytes.toString(CellUtil.cloneValue(rawCell)));
                                }
                                //如果文件id等于当前要删除的fileId，则加入要删除的数组
                                if (Bytes.toString(CellUtil.cloneValue(rawCell)).equals(fileId)) {
                                    Delete groupDelete = new Delete(CellUtil.cloneRow(rawCell));
                                    groupDelete.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_fileId), rawCell.getTimestamp());
                                    groupDeList.add(groupDelete);
                                }
                            }
                        }
                    }
                }
            }

            //针对删除的文件夹获得其中的所有的fileId
            List<String> fielIdList=new ArrayList<>();
            fielIdList.add(fileId);
            //递归获取到该文件夹下所有要删除的文件
            deleteCallBack(fileTable,fielIdList,fileId,uId,gId);

            //查询file表获得对应的时间戳
            for (String fileid : fielIdList) {
                Get fileGet = new Get(Bytes.toBytes(fileid));
                fileGet.setMaxVersions();
                fileGet.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth));
                Result fileAuthRes = fileTable.get(fileGet);
                if (!fileAuthRes.isEmpty()) {
                    for (Cell cell : fileAuthRes.rawCells()) {
                        //如果包含了文件权限表的权限，则进行删除
                        if (authIdList.contains(Bytes.toString(CellUtil.cloneValue(cell)))) {
                            Delete delete = new Delete(Bytes.toBytes(fileid));
                            delete.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth), cell.getTimestamp());
                            deleteList.add(delete);
                        }
                    }
                }
            }
            indexTable.delete(indexDeList);
            groupTable.delete(groupDeList);
            fileTable.delete(deleteList);
            indexTable.close();
            fileTable.close();
            groupTable.close();
        } catch (Exception e) {
          logger.error(e.getMessage());
            return false;
        } finally {
            if (hbaseConnection != null) {
                HbaseConnectionPool.releaseConnection(hbaseConnection);
            }
        }
        return true;
    }


    //递归获取要删除的文件夹下面所有的文件id
    private static void deleteCallBack(Table fileTable, List<String> fielIdList, String fileId, String uId, String gId) throws IOException {
        Scan scan = new Scan();
        FilterList filterList = new FilterList();
        Filter colFilter = new PrefixFilter(Bytes.toBytes(uId));
        SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                Static.FILE_TABLE_CF.getBytes(),
                Static.FILE_TABLE_BACK.getBytes(),
                CompareFilter.CompareOp.EQUAL,
                new BinaryComparator(Bytes.toBytes(fileId)));
        singleColumnValueFilter.setFilterIfMissing(true);
        filterList.addFilter(colFilter);
        filterList.addFilter(singleColumnValueFilter);

        scan.setFilter(filterList);
        ResultScanner scanner = fileTable.getScanner(scan);
        for (Result result : scanner) {
            String newFile = Bytes.toString(result.getRow());
            fielIdList.add(newFile);
            deleteCallBack(fileTable, fielIdList, newFile, uId, gId);
        }
    }
}
