package com.shu.hbase.test;

import com.shu.hbase.pojo.Static;
import com.shu.hbase.tools.Get;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@CrossOrigin
public class TestController {

    private Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("testPublicFile")
    public String testPublicFile() {


        for (int i = 0; i < 15; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("新的线程启动了" + Thread.currentThread().getName());
                    final String url = "http://localhost:8080/getPublicFiles";
                    String string = Get.sendGet(url, "", "");
                    System.out.println(string);
                }
            }).start();
        }

        return "success";
    }


    @GetMapping("testSearchFile")
    public String downLoadTest() {
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("新的线程启动了" + Thread.currentThread().getName());
                    final String url = "http://localhost:8080/searchFile";
                    String string = Get.sendGet(url, "value=3&type=share", "");
                    System.out.println(string);
                }
            }).start();
        }

        return "success";
    }


    @GetMapping("testBuildDir")
    public String testBuildDir() {
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("新的线程启动了" + Thread.currentThread().getName());
                    final String url = "http://localhost:8080/buildDirect";
                    String string = Get.sendGet(url, "backId=19721631&dirName=testDir" + new java.util.Random().nextInt(900) + 100, "");
                    System.out.println(string);
                }
            }).start();
        }
        return "success";
    }


    /**
     * 输出file表的数据
     *
     * @param
     * @throws IOException
     */
    @GetMapping("filesTable")
    public void fileTable() throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Table table = connection.getTable(TableName.valueOf(Static.FILE_TABLE));
        ResultScanner scanner = table.getScanner(new Scan().setMaxVersions());

        System.out.println("file表的数据为：-------------------------------------------------------");
        for (Result result : scanner) {
            System.out.println("新的一行" + Bytes.toString(result.getRow()));
            for (Cell cell : result.rawCells()) {
                System.out.print("列名为" + Bytes.toString(CellUtil.cloneQualifier(cell)) + "  ");
                System.out.println(Bytes.toString(CellUtil.cloneValue(cell)));
            }
        }
        HbaseConnectionPool.releaseConnection(connection);
    }

    /**
     * 测试index表的数据
     *
     * @param
     * @throws IOException
     */
    @GetMapping("indexTable")
    public void indexTable() throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Table table = connection.getTable(TableName.valueOf(Static.INDEX_TABLE));
        ResultScanner scanner = table.getScanner(new Scan().setMaxVersions());
        System.out.println("index表的数据为：-------------------------------------------------------");
        if (scanner != null) {
            for (Result result : scanner) {
                System.out.println("新的一行" + Bytes.toString(result.getRow()));
                for (Cell cell : result.rawCells()) {
                    System.out.print("列名为" + Bytes.toString(CellUtil.cloneQualifier(cell)) + "  ");
                    System.out.println(Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
        }


        HbaseConnectionPool.releaseConnection(connection);
    }

    /**
     * 测试group表的数据
     *
     * @param
     * @throws IOException
     */
    @GetMapping("groupTable")
    public void groupTable() throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Table table = connection.getTable(TableName.valueOf(Static.GROUP_TABLE));
        ResultScanner scanner = table.getScanner(new Scan().setMaxVersions());
        System.out.println("group表的数据为：-------------------------------------------------------");
        for (Result result : scanner) {
            System.out.println("新的一行" + Bytes.toString(result.getRow()));
            for (Cell cell : result.rawCells()) {
                System.out.print("列名为" + Bytes.toString(CellUtil.cloneQualifier(cell)) + "  ");
                System.out.println(Bytes.toString(CellUtil.cloneValue(cell)));
            }
        }
        HbaseConnectionPool.releaseConnection(connection);
    }

    /**
     * 测试user表的数据
     *
     * @param
     * @throws IOException
     */
    @GetMapping("userTable")
    public void buildDirect() throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Table table = connection.getTable(TableName.valueOf(Static.USER_TABLE));
        ResultScanner scanner = table.getScanner(new Scan().setMaxVersions());
        System.out.println("user表的数据为：-------------------------------------------------------");
        for (Result result : scanner) {
            System.out.println("新的一行" + Bytes.toString(result.getRow()));
            for (Cell cell : result.rawCells()) {
                System.out.print("列名为" + Bytes.toString(CellUtil.cloneQualifier(cell)) + "  ");
                System.out.println(Bytes.toString(CellUtil.cloneValue(cell)));
            }
        }
        HbaseConnectionPool.releaseConnection(connection);
    }

}
