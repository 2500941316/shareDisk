package com.shu.hbase.test;

import com.shu.hbase.pojo.Static;
import com.shu.hbase.tools.Get;
import com.shu.hbase.tools.Post;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import com.shu.hbase.tools.hdfspool.HdfsConnectionPool;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
                    String string = Get.sendGet(url, "backId=19721631&dirName=testDir" + Thread.currentThread().getName()+System.currentTimeMillis(), "");
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


    /**
     * 删除user表
     *
     * @param
     * @throws IOException
     */
    @GetMapping("deleteUserTable")
    public void deleteUserTable() throws Exception {
       deleteTable("shuwebfs:user");
    }

    /**
     * 创建user表
     *
     * @param
     * @throws IOException
     */
    @GetMapping("buildUserTable")
    public void buildUserTable() throws Exception {
       buildTable(Static.USER_TABLE, new String[]{Static.USER_TABLE_CF});
    }


    /**
     * 查询hdfs的文件
     *
     * @param
     * @throws IOException
     */
    @GetMapping("getHdfs")
    public void getHdfs() throws Exception {
        FileSystem fs = HdfsConnectionPool.getHdfsConnection();
        FileStatus[] fileStatuses = fs.listStatus(new Path("/shuwebfs/19721631/"));
        for (FileStatus fileStatus : fileStatuses) {
            System.out.println(fileStatus.getPath().getName());
        }
        HdfsConnectionPool.releaseConnection(fs);
    }

    /**
     * 删除所有表的数据
     *
     * @param
     * @throws IOException
     */
    @GetMapping("deleteAll")
    public void deleteAll() throws Exception {
        clearTables();
    }

    /**
     * 删除hdfs下的所以文件
     *
     * @param
     * @throws IOException
     */
    @GetMapping("deleteHdfs")
    public void deleteHdfs() throws Exception {
        FileSystem fs = HdfsConnectionPool.getHdfsConnection();
        fs.delete(new Path("/shuwebfs/19721631/"), true);
        HdfsConnectionPool.releaseConnection(fs);
    }

    /**
     * 给首页传值
     *
     * @param
     * @throws IOException
     */
    @GetMapping("postToindex")
    public void postToindex() throws Exception {
        Post.Post();
    }


    public static void clearTables() throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Table table1 = connection.getTable(TableName.valueOf(Static.FILE_TABLE));
        Table table2 = connection.getTable(TableName.valueOf(Static.GROUP_TABLE));
        Table table3 = connection.getTable(TableName.valueOf(Static.INDEX_TABLE));
        Table table4 = connection.getTable(TableName.valueOf(Static.USER_TABLE));
        Scan scan=new Scan();

        Iterator<Result> iterator1 = table1.getScanner(scan).iterator();
        listDeleteRow(iterator1,table1);
        Iterator<Result> iterator2 = table2.getScanner(scan).iterator();
        listDeleteRow(iterator2,table2);
        Iterator<Result> iterator3 = table3.getScanner(scan).iterator();
        listDeleteRow(iterator3,table3);
        Iterator<Result> iterator4 = table4.getScanner(scan).iterator();
        listDeleteRow(iterator4,table4);
        HbaseConnectionPool.releaseConnection(connection);
    }

    public static void listDeleteRow(Iterator<Result> iterator,Table table) throws Exception {
        List<Delete> deleteList=new ArrayList<>();
        while (iterator.hasNext())
        {
            Result result = iterator.next();

            Delete delete=new Delete(result.getRow());
            deleteList.add(delete);
        }
        table.delete(deleteList);
        System.out.println(table.getName().toString()+"删除了数据数量为："+deleteList.size());
    }


    public static void buildTable(String tableName, String columnFamily[]) throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Admin admin = connection.getAdmin();
        // 添加列族
        HTableDescriptor desc = new HTableDescriptor(TableName.valueOf(tableName));
        for (int i = 0; i < columnFamily.length; i++) {
            desc.addFamily(new HColumnDescriptor(columnFamily[i]).setMaxVersions(1));

        }

        // 如果表存在就是先disable，然后在delete
        if (admin.tableExists(TableName.valueOf(tableName))) {
            admin.disableTable(TableName.valueOf(tableName));
            admin.deleteTable(TableName.valueOf(tableName));
        }
        // 创建表
        admin.createTable(desc);
        HbaseConnectionPool.releaseConnection(connection);
    }


    public static void deleteTable(String tableName) throws Exception {
        Connection connection = HbaseConnectionPool.getHbaseConnection();
        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {
            admin.disableTable(TableName.valueOf(tableName));
            admin.deleteTable(TableName.valueOf(tableName));
        } else {
            System.out.println("table \"" + tableName + "\" is not exist!");
        }
        HbaseConnectionPool.releaseConnection(connection);
    }


}
