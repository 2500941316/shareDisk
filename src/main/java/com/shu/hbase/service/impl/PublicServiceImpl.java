package com.shu.hbase.service.impl;

import com.shu.hbase.exceptions.BusinessException;
import com.shu.hbase.exceptions.Exceptions;
import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import com.shu.hbase.service.interfaces.PublicService;
import com.shu.hbase.tools.TableModel;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
            tableModel.setData(200);
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
}
