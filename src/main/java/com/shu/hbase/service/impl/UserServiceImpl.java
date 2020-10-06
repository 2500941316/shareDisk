package com.shu.hbase.service.impl;

import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import com.shu.hbase.service.impl.upload.MvcToHadoop;
import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
                return TableModel.error("您的访问权限不足");
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
        if (backId.length() > 8) {
            if (!backId.substring(0, 8).equals(userId)) {
                return TableModel.error("权限不足");
            }
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
        FileSystem fs = null;
        try {
            fs = HdfsConnectionPool.getHdfsConnection();
            fs.mkdirs(new Path(path));

            insertToFiles(null, "dir", path, backId, userId, userId + "_" + uniqueseed.incrementAndGet());
            logger.info("insert执行成功");
            HdfsConnectionPool.releaseConnection(fs);
            logger.info("文件夹创建成功");
        } catch (Exception e) {
            logger.error(e.getMessage());
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


}
