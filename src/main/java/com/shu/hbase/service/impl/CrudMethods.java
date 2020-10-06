package com.shu.hbase.service.impl;

import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
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
}
