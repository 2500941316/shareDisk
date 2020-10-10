package com.shu.hbase.service.impl;

import com.shu.hbase.pojo.*;
import com.shu.hbase.service.impl.upload.MvcToHadoop;
import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import com.shu.hbase.tools.hdfspool.HdfsConnectionPool;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.shu.hbase.service.impl.CrudMethods.*;

@Service
public class UserServiceImpl implements UserService {
    private static Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static AtomicLong uniqueseed = new AtomicLong(System.currentTimeMillis());

    //根据fileId在file表中查找文件信息
    public TableModel selectFile(String backId, String type, String uId, String gId) {
        TableModel tableModel = new TableModel();
        Connection hBaseConn = null;
        Table fileTable = null;
        Table userTable = null;
        List<FileInfoVO> list = new ArrayList<>();
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            userTable = hBaseConn.getTable(TableName.valueOf(Static.USER_TABLE));
            logger.info("开始检测用户查询权限");
            //权限检测
            if (!verifite(fileTable, uId, backId, gId)) {
                logger.info("权限检测失败！正在返回");
                tableModel.setMsg("auth检测失败，权限不足！");
                return tableModel;
            }
            logger.info("权限检测成功");
            Scan scan = new Scan();
            if (type.equals("0")) {
                logger.info("查询上一级的文件信息");
                //如果是返回上一级，则查询backid的backid，查询backid即可
                Get get = new Get(Bytes.toBytes(backId));
                get.setMaxVersions();
                get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_BACK));
                Result result = fileTable.get(get);
                if (!result.isEmpty()) {
                    Cell cell = result.rawCells()[0];
                    String lastBack = Bytes.toString(CellUtil.cloneValue(cell));
                    HbaseConnectionPool.releaseConnection(hBaseConn);
                    return selectFile(lastBack, "1", uId, gId);
                }
            } else {
                logger.info("查询下一级目录的文件");
                SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                        Static.FILE_TABLE_CF.getBytes(),
                        Static.FILE_TABLE_BACK.getBytes(),
                        CompareFilter.CompareOp.EQUAL,
                        new BinaryComparator(Bytes.toBytes(backId)));
                singleColumnValueFilter.setFilterIfMissing(true);
                scan.setFilter(singleColumnValueFilter);
                scan.setMaxVersions();
                ResultScanner results = fileTable.getScanner(scan);
                for (Result result : results) {
                    FileInfoVO fileInfoVO = packageCells(result);
                    list.add(fileInfoVO);
                }
            }
            logger.info("查询成功，开始封装数据");
            //获取我的文件夹总大小
            Get get = new Get(Bytes.toBytes(uId));
            get.addColumn(Bytes.toBytes(Static.USER_TABLE_CF), Bytes.toBytes(Static.USER_TABLE_SIZE));
            Result result = userTable.get(get);
            if (!result.isEmpty()) {
                for (Cell cell : result.rawCells()) {
                    if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.USER_TABLE_SIZE)) {
                        tableModel.setMsg(Bytes.toString(CellUtil.cloneValue(cell)));
                    }
                }
            }
            tableModel.setCount(list.size());
            tableModel.setData(list);
            tableModel.setCode(0);
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {
            try {
                assert fileTable != null;
                fileTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }
        return tableModel;
    }


    //获取文件类型
    public TableModel getFilesByType(String type, String uId) {
        Connection hBaseConn = null;
        Table fileTable = null;
        try {
            if (!uId.substring(0, 8).equals(uId)) {
                return TableModel.error("权限不足");
            }
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            logger.info("开始查询用户id开头的满足类型的文件");
            Scan scan = new Scan();
            FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
            Filter colFilter = new PrefixFilter(Bytes.toBytes(uId));
            SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                    Static.FILE_TABLE_CF.getBytes(),
                    Static.FILE_TABLE_TYPE.getBytes(),
                    CompareFilter.CompareOp.EQUAL,
                    new BinaryComparator(Bytes.toBytes(type)));
            singleColumnValueFilter.setFilterIfMissing(true);
            filterList.addFilter(colFilter);
            filterList.addFilter(singleColumnValueFilter);
            scan.setFilter(filterList);
            ResultScanner scanner = fileTable.getScanner(scan);
            List<FileInfoVO> list = new ArrayList<>();
            for (Result result : scanner) {
                FileInfoVO fileInfoVO = packageCells(result);
                list.add(fileInfoVO);
            }
            TableModel tableModel = new TableModel();
            tableModel.setData(list);
            tableModel.setCount(list.size());
            tableModel.setCode(0);
            return tableModel;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TableModel.error("参数有误");
        } finally {
            try {
                assert fileTable != null;
                fileTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }


    //接收上传的文件到后端服务器
    @Override
    public TableModel uploadTomvc(MultipartFile file, Integer chunk, Integer chunks, String uid, HttpServletRequest request, String backId) {
        logger.info("接收到上传的文件，开始执行上传逻辑");
        //获取项目的根路径
        String realpath = request.getSession().getServletContext().getRealPath("/");
        String fileId = uid + "_" + System.currentTimeMillis();
        //截取上传文件的类型
        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        File video = new File(realpath + "final" + uid + "/" + file.getOriginalFilename());
        logger.info("截取上传文件类型成功");
        //先判断文件的父目录是否存在，不存在需要创建；否则报错
        try {
            if (!video.getParentFile().exists()) {
                video.getParentFile().mkdirs();
                video.createNewFile();//创建文件
            }
            if (chunk == null && chunks == null) {//没有分片 直接保存
                logger.info("文件没有分片，直接保存");
                file.transferTo(video);
                return MvcToHadoop.createFile(realpath + "final" + uid + "/" + file.getOriginalFilename(), backId, fileId, uid);
            } else {
                logger.info("文件分片，新建临时保存文件夹");
                //根据guid 创建一个临时的文件夹
                File file2 = new File(realpath + "/" + uid + "/" + file.getOriginalFilename() + "/" + chunk + fileType);
                if (!file2.exists()) {
                    file2.mkdirs();
                }

                //保存每一个分片
                file.transferTo(file2);

                //如果当前是最后一个分片，则合并所有文件
                if (chunk == (chunks - 1)) {
                    logger.info("开始保存最后一个分片");
                    File tempFiles = new File(realpath + "/" + uid + "/" + file.getOriginalFilename());
                    File[] files = tempFiles.listFiles();
                    while (true) {
                        assert files != null;
                        if (files.length == chunks) {
                            break;
                        }
                        Thread.sleep(300);
                        files = tempFiles.listFiles();
                    }
                    FileOutputStream fileOutputStream = null;
                    BufferedOutputStream bufferedOutputStream = null;
                    BufferedInputStream inputStream = null;
                    try {
                        logger.info("开始进行写入hdfs的准备工作");
                        //创建流
                        fileOutputStream = new FileOutputStream(video, true);
                        //创建文件输入缓冲流
                        bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                        byte[] buffer = new byte[4096];//一次读取1024个字节
                        //对这个文件数组进行排序
                        logger.info("对文件数组进行排序");
                        Arrays.sort(files, new Comparator<File>() {
                                    @Override
                                    public int compare(File o1, File o2) {
                                        int o1Index = Integer.parseInt(o1.getName().split("\\.")[0]);
                                        int o2Index = Integer.parseInt(o2.getName().split("\\.")[0]);
                                        return Integer.compare(o1Index, o2Index);
                                    }
                                }
                        );
                        logger.info("开始将分片的文件合成为一个主文件");
                        for (File fileTemp : files) {
                            inputStream = new BufferedInputStream(new FileInputStream(fileTemp));
                            int readcount;
                            while ((readcount = inputStream.read(buffer)) > 0) {
                                bufferedOutputStream.write(buffer, 0, readcount);
                                bufferedOutputStream.flush();
                            }
                            inputStream.close();
                        }
                        bufferedOutputStream.close();
                        logger.info("分片文件合成成功！");
                        logger.info("开始写入hdfs");
                        return MvcToHadoop.createFile(realpath + "final" + uid + "/" + file.getOriginalFilename(), backId, fileId, uid);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return TableModel.error("上传失败");
                    } finally {
                        for (File value : files) {
                            value.delete();
                        }
                        tempFiles.delete();
                        assert inputStream != null;
                        inputStream.close();
                        fileOutputStream.close();
                        bufferedOutputStream.close();
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return TableModel.error("网络异常");
    }


    //目录的创建
    @Override
    public TableModel buildDirect(String backId, String dirName, String userId) {
        String path = "";
        logger.info("创建文件夹权限验证");
        Connection hBaseConn = null;
        FileSystem fs = null;
        Table fileTable = null;
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            if (backId.length() > 8) {
                if (!backId.substring(0, 8).equals(userId)) {
                    return TableModel.error("权限不足");
                }
            }
            if (!verifite(fileTable, userId, backId, null)) {
                logger.info("权限检测失败！正在返回");
                return TableModel.error("auth检测失败，权限不足！");
            }
            logger.info("创建文件夹权限验证成功");
            //如果是创建默认文件夹则拼接路径
            if (dirName.equals("/我的文档")) {
                path = "/shuwebfs/" + userId + dirName;
            } else {
                path = findUploadPath(backId);
                assert path != null;
                if (!path.isEmpty()) {
                    path = path + "/" + dirName;
                }
            }
            logger.info("新建文件夹物理路径拼接");
            fs = HdfsConnectionPool.getHdfsConnection();
            fs.mkdirs(new Path(path));
            insertToFiles(null, "dir", path, backId, userId, userId + "_" + uniqueseed.incrementAndGet());
            logger.info("文件夹创建成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                assert fileTable != null;
                fileTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
                if (fs != null)
                    HdfsConnectionPool.releaseConnection(fs);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
        return TableModel.success("创建成功");
    }


    //删除我的文件
    @Override
    public TableModel deleteFile(String fileId, String uId) {
        logger.info("执行删除权限检测");
        if (!fileId.substring(0, 8).equals(uId)) {
            return TableModel.error("权限不足");
        }

        Connection hBaseConn = null;
        Table fileTable = null;
        Table userTable = null;
        List<Integer> sizeList = new ArrayList<>();
        String path = null;
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            userTable = hBaseConn.getTable(TableName.valueOf(Static.USER_TABLE));
            //首先根据id查出文件物理路径，调用hdfs的方法删除文件
            Get get = new Get(Bytes.toBytes(fileId));
            get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_PATH));
            get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_SIZE));
            Result result = fileTable.get(get);
            logger.info("查询删除文件的物理地址");
            if (!result.isEmpty()) {
                for (Cell cell : result.rawCells()) {
                    if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.FILE_TABLE_PATH))
                        path = Bytes.toString(CellUtil.cloneValue(cell));
                    if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.FILE_TABLE_SIZE)) {
                        if (!Bytes.toString(CellUtil.cloneValue(cell)).equals("-")) {
                            sizeList.add(Integer.parseInt(Bytes.toString(CellUtil.cloneValue(cell))));
                        }
                    }
                }
                logger.info("执行在hdfs中删除物理文件的方法");
                if (delete(path)) {
                    logger.info("hdfs中文件和文件夹删除成功");
                    //当hdfs和共享组中删除完毕后，递归删除文件表中的该fileId下面的所有文件
                    List<Delete> deleteList = new ArrayList<>();
                    logger.info("通过文件id删除开始在hbase中删除文件");
                    deleteFilesById(fileTable, deleteList, fileId, uId, sizeList);
                    deleteList.add(new Delete(Bytes.toBytes(fileId)));
                    if (!deleteList.isEmpty()) {
                        fileTable.delete(deleteList);
                    }
                    int sizeAll = 0;
                    for (Integer integer : sizeList) {
                        sizeAll += integer;
                    }
                    if (!insertOrUpdateUser(userTable, sizeAll + "", uId, "delete")) {
                        return TableModel.error("文件超出存储容量");
                    }
                }
            }
            logger.info("文件删除成功");
            return TableModel.success("删除成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TableModel.error("删除失败");
        } finally {
            try {
                assert fileTable != null;
                fileTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }


    //获取当前用户的共享组
    @Override
    public TableModel getShares(String uid) {
        Connection hBaseConn = null;
        Table indexTable = null;
        Table groupTable = null;
        try {
            if (StringUtils.isEmpty(uid)) {
                throw new Exception("参数有误");
            }
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            //获得index表下面所有组的id
            logger.info("开始获取index表中所以分组的id");
            indexTable = hBaseConn.getTable(TableName.valueOf(Static.INDEX_TABLE));
            groupTable = hBaseConn.getTable(TableName.valueOf(Static.GROUP_TABLE));
            Get indexGet = new Get(Bytes.toBytes(uid));
            indexGet.addFamily(Bytes.toBytes(Static.INDEX_TABLE_CF));
            Result result = indexTable.get(indexGet);
            logger.info("获得到当前用户所以分组的id，数量为" + result.size());
            List<GroupInfoVO> groupInfoVOList = new ArrayList<>();
            for (Cell cell : result.rawCells()) {
                logger.info("对每个分组id进行查询，封装为组信息对象");
                GroupInfoVO groupInfoVO = new GroupInfoVO();
                Set<String> memberSet = new HashSet<>();
                Set<ShareFileVO> fileVOSet = new HashSet<>();
                //在group表中，根据组id，搜索出对应的name、path 和member
                logger.info("使用每个组id在group表中查询组名称 member 文件id");
                Get groupGet = new Get(CellUtil.cloneQualifier(cell));
                groupInfoVO.setGId(Bytes.toString(CellUtil.cloneQualifier(cell)));
                groupGet.setMaxVersions();
                groupGet.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_NAME));
                groupGet.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_MEMBER));
                groupGet.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_fileId));
                Result groupRes = groupTable.get(groupGet);
                logger.info("开始对每个分组的查询结果进行封装");
                for (Cell rawCell : groupRes.rawCells()) {
                    if (Bytes.toString(CellUtil.cloneQualifier(rawCell)).equals(Static.GROUP_TABLE_MEMBER)) //如果列名是member
                    {
                        memberSet.add(Bytes.toString(CellUtil.cloneValue(rawCell)));
                    } else if (Bytes.toString(CellUtil.cloneQualifier(rawCell)).equals(Static.GROUP_TABLE_NAME))  //如果列名name
                    {
                        groupInfoVO.setName(Bytes.toString(CellUtil.cloneValue(rawCell)));
                    } else {
                        String path = Bytes.toString(CellUtil.cloneValue(rawCell));
                        ShareFileVO shareFileVO = new ShareFileVO();
                        //截取最后一个/，之后的是文件名称
                        String fileName = path.substring(path.lastIndexOf("/") + 1);
                        shareFileVO.setName(fileName);
                        shareFileVO.setPath(path);
                        if (path.charAt(1) == '/' && path.charAt(10) == '/') {
                            shareFileVO.setSharer(path.substring(2, 10));
                        }
                        fileVOSet.add(shareFileVO);
                    }
                }
                logger.info("分组对象封装成功，开始返回数据");
                groupInfoVO.setMember(memberSet);
                groupInfoVO.setFile(fileVOSet);
                groupInfoVOList.add(groupInfoVO);
            }
            TableModel tableModel = new TableModel();
            tableModel.setData(groupInfoVOList);
            tableModel.setCode(200);
            return tableModel;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TableModel.error("网络异常，查询失败");
        } finally {
            try {
                if (indexTable != null)
                    indexTable.close();
                if (groupTable != null)
                    groupTable.close();
                if (hBaseConn != null)
                    HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }


    //新建分组
    @Override
    public TableModel buildGroup(NewGroupInfoVO newGroupInfoVO) {
        Connection hBaseConn = null;
        Table groupTable = null;
        Table indexTable = null;
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            groupTable = hBaseConn.getTable(TableName.valueOf(Static.GROUP_TABLE));
            indexTable = hBaseConn.getTable(TableName.valueOf(Static.INDEX_TABLE));

            //在group表中，将分组建立
            logger.info("开始执行创建分组方法");
            logger.info("获得当前时间戳");
            long l = System.currentTimeMillis();
            String groupId = newGroupInfoVO.getUId() + l;
            logger.info("将用户id和时间戳合并成分组id");
            Put groupPut = new Put(Bytes.toBytes(groupId));
            groupPut.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_NAME), Bytes.toBytes(newGroupInfoVO.getGroupName()));

            logger.info("将分组成员添加到不同的时间戳上");
            for (int i = 0; i < newGroupInfoVO.getMember().size(); i++) {
                groupPut.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_MEMBER), l + i, Bytes.toBytes(newGroupInfoVO.getMember().get(i)));
            }
            groupTable.put(groupPut);

            logger.info("开始想index表中添加一个列，列名叫做分组的id");
            //在index表中，针对每一个member，添加一个列
            List<Put> putList = new ArrayList<>();
            for (String member : newGroupInfoVO.getMember()) {
                Put indexPut = new Put(Bytes.toBytes(member));
                indexPut.addColumn(Bytes.toBytes(Static.INDEX_TABLE_CF), Bytes.toBytes(groupId), null);
                putList.add(indexPut);
            }
            indexTable.put(putList);
            logger.info("创建分组成功");
            return TableModel.success("创建成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TableModel.error("网络异常，请重试");
        } finally {
            try {
                assert groupTable != null;
                groupTable.close();
                assert indexTable != null;
                indexTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    //获得某一个分组的文件
    @Override
    public TableModel getGroupFile(String gId) {
        Connection hBaseConn = null;
        Table groupTable = null;
        Table fileTable = null;
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            groupTable = hBaseConn.getTable(TableName.valueOf(Static.GROUP_TABLE));
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            //根据gid查询每个组的文件
            logger.info("根据分组id查询分组的文件fileid");
            Get get = new Get(Bytes.toBytes(gId));
            get.setMaxVersions();
            get.addColumn(Bytes.toBytes(Static.GROUP_TABLE_CF), Bytes.toBytes(Static.GROUP_TABLE_fileId));
            Result result = groupTable.get(get);
            logger.info("group表查询成功，分组中含有文件数量为" + result.size());
            List<FileInfoVO> fileVOList = new ArrayList<>();
            logger.info("根据查询到的fileid，在file表中查询对应的文件具体信息");
            for (Cell cell : result.rawCells()) {
                String fileId = Bytes.toString(CellUtil.cloneValue(cell));
                //根据fileId，查询出file表中对应的文件
                Get fileGet = new Get(Bytes.toBytes(fileId));
                fileGet.addFamily(Bytes.toBytes(Static.FILE_TABLE_CF));
                Result fileRes = fileTable.get(fileGet);
                if (!fileRes.isEmpty()) {
                    FileInfoVO fileInfoVO = packageCells(fileRes);
                    fileVOList.add(fileInfoVO);
                }
            }
            logger.info("文件具体信息封装完毕，开始返回");
            TableModel tableModel = new TableModel();
            tableModel.setData(fileVOList);
            tableModel.setCount(fileVOList.size());
            tableModel.setCode(0);
            return tableModel;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TableModel.error("网络异常请重试");
        } finally {
            try {
                assert groupTable != null;
                groupTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

}
