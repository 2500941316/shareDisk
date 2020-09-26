package com.shu.hbase.service.impl;

import com.shu.hbase.exceptions.BusinessException;
import com.shu.hbase.exceptions.Exceptions;
import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import com.shu.hbase.service.impl.downLoad.DownLoad;
import com.shu.hbase.service.interfaces.PublicService;
import com.shu.hbase.tools.TableModel;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import com.shu.hbase.tools.hdfspool.HdfsConnectionPool;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


@Service
public class PublicServiceImpl implements PublicService {
    private Logger logger = LoggerFactory.getLogger(PublicServiceImpl.class);

    @Override
    public TableModel getPublicFiles(String uid) {
        Connection hBaseConn = null;
        Table fileTable = null;
        Table userTable = null;
        TableModel tableModel = new TableModel();
        if (StringUtils.isEmpty(uid)) {
            return TableModel.error("参数有误");
        }
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            userTable = hBaseConn.getTable(TableName.valueOf(Static.USER_TABLE));

            //查询user大小表中没有用户rowkey，如果没有则插入
            if (!CrudMethods.insertOrUpdateUser(userTable, "0", uid, "upload")) {
                return TableModel.error("文件超出存储容量");
            }
            logger.info("容量检测成功");
            logger.info("开始查询公共共享文件");
            //查询公共文件的信息，并且进行封装
            List<FileInfoVO> fileInfoVOS = new ArrayList<>();

            FilterList filterListNew = new FilterList(FilterList.Operator.MUST_PASS_ALL);
            Filter colFilterNew = new PrefixFilter(Bytes.toBytes("00000000"));
            Scan newScan = new Scan();
            newScan.setReversed(true);
            newScan.setCaching(1);
//            Filter filter = new PageFilter(8);
//            filterListNew.addFilter(filter);
            filterListNew.addFilter(colFilterNew);
            newScan.setFilter(filterListNew);
            ResultScanner scanner2 = fileTable.getScanner(newScan);
            logger.info("共享文件查询成功");
            logger.info("开始封装查询信息");
            // 返回查询遍历器
            for (Result result : scanner2) {
                FileInfoVO fileInfoVO = CrudMethods.packageCells(result);
                fileInfoVOS.add(fileInfoVO);
            }
            logger.info("查询成功！");
            tableModel.setCount(fileInfoVOS.size());
            tableModel.setCode(0);
            tableModel.setData(fileInfoVOS);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BusinessException(Exceptions.SERVER_OTHER_ERROR.getEcode());

        } finally {
            try {
                assert fileTable != null;
                fileTable.close();
                assert userTable != null;
                userTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return tableModel;
    }

    //查找框查找方法
    public TableModel searchFile(String value, String uId) {
        Connection hBaseConn = null;
        Table fileTable = null;
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            List<FileInfoVO> list = new ArrayList<>();
            logger.info("开始查询相关的公共共享文件");
            Scan scan = new Scan();
            FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
            Filter colFilter = new PrefixFilter(Bytes.toBytes(uId));

            SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                    Static.FILE_TABLE_CF.getBytes(),
                    Static.FILE_TABLE_NAME.getBytes(),
                    CompareFilter.CompareOp.EQUAL,
                    new SubstringComparator(value));
            singleColumnValueFilter.setFilterIfMissing(true);
            filterList.addFilter(colFilter);
            filterList.addFilter(singleColumnValueFilter);
            scan.setFilter(filterList);
            ResultScanner scanner = fileTable.getScanner(scan);
            for (Result result : scanner) {
                if (!result.isEmpty()) {
                    FileInfoVO fileInfoVO = CrudMethods.packageCells(result);
                    list.add(fileInfoVO);
                }
            }
            logger.info("关键词查询成功！");

            TableModel tableModel = new TableModel();
            tableModel.setCount(list.size());
            tableModel.setCode(0);
            tableModel.setData(list);
            return tableModel;
        } catch (Exception e) {
            logger.info(e.getMessage());
            return TableModel.error("error");
        } finally {
            HbaseConnectionPool.releaseConnection(hBaseConn);
        }
    }


    @Override
    public void downLoad(String fileId, String gId, HttpServletResponse response, HttpServletRequest request, String uid) {
        //从files表中查询出下载文件的物理地址，然后调用下载函数
        Connection hBaseConn = null;
        FileSystem fs = null;
        Table fileTable = null;
        try {
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            fs = HdfsConnectionPool.getHdfsConnection();
            fileTable = hBaseConn.getTable(TableName.valueOf(Static.FILE_TABLE));
            logger.info("开始验证用户下载权限");
            //权限验证
            if (!CrudMethods.verifite(fileTable, uid, fileId, gId)) {
                return;
            }
            logger.info("用户下载权限校验成功");
            Get get = new Get(Bytes.toBytes(fileId));
            get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_PATH));
            Result result = fileTable.get(get);
            logger.info("开始根据文件id查询文件存储路径");
            if (!result.isEmpty()) {
                Cell cell = result.rawCells()[0];
                String path = Bytes.toString(CellUtil.cloneValue(cell));
                logger.info("调用下载方法");
                DownLoad.downloadFromHDFSinOffset(fs, response, path, request);
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {
            try {
                fileTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
                HdfsConnectionPool.releaseConnection(fs);
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }
    }
}
