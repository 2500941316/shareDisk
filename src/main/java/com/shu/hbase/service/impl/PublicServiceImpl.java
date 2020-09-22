package com.shu.hbase.service.impl;

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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Service
public class PublicServiceImpl implements PublicService {

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

            //查询公共文件的信息，并且进行封装
            List<FileInfoVO> fileInfoVOS = new ArrayList<>();

            FilterList filterListNew = new FilterList(FilterList.Operator.MUST_PASS_ALL);
            Filter colFilterNew = new PrefixFilter(Bytes.toBytes("00000000"));
            Scan newScan = new Scan();
            newScan.setReversed(true);
            newScan.setCaching(1);
            Filter filter = new PageFilter(7);
            filterListNew.addFilter(colFilterNew);
            filterListNew.addFilter(filter);
            newScan.setFilter(filterListNew);
            ResultScanner scanner2 = fileTable.getScanner(newScan);

            Iterator<Result> res = scanner2.iterator();// 返回查询遍历器
            while (res.hasNext()) {
                Result result = res.next();
                FileInfoVO fileInfoVO = CrudMethods.packageCells(result);
                fileInfoVOS.add(fileInfoVO);
            }

            tableModel.setData(200);
            tableModel.setData(fileInfoVOS);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileTable.close();
                userTable.close();
                HbaseConnectionPool.releaseConnection(hBaseConn);
            } catch (Exception e) {
            }
        }
        return tableModel;
    }
}
