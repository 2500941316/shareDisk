package com.shu.hbase.service.impl;

import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

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
            if (!CrudMethods.verifite(fileTable, uId, backId, gId)) {
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
                    FileInfoVO fileInfoVO = CrudMethods.packageCells(result);
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
                FileInfoVO fileInfoVO = CrudMethods.packageCells(result);
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
                fileTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

}
