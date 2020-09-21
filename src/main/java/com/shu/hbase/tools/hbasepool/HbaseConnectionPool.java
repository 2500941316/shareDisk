package com.shu.hbase.tools.hbasepool;

import com.shu.hbase.tools.filetype.FileTypeJudge;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.hadoop.hbase.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HbaseConnectionPool {
    private static ConnectionFactory factory;
    private static ConnectionPoolConfig connectionPool;
    private static GenericObjectPoolConfig config;
    private static Logger logger = LoggerFactory.getLogger(FileTypeJudge.class);

    static {
        factory = new ConnectionFactory();
        config = new GenericObjectPoolConfig();
        config.setMaxTotal(30);
        //最小空闲数
        config.setMinIdle(2);
        config.setTimeBetweenEvictionRunsMillis(60000);
        //最大空闲数
        config.setMaxIdle(8);
        config.setMaxWaitMillis(10000);
        connectionPool = new ConnectionPoolConfig(factory, config);
    }

    public static Connection getHbaseConnection() throws Exception {
        try {
            // System.out.println("归还了"+connectionPool.getReturnedCount());
            // System.out.println("借出了"+connectionPool.getBorrowedCount());
            return connectionPool.borrowObject();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public static void releaseConnection(Connection connection) {
        try {
            //System.out.println("归还了连接");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            connectionPool.returnObject(connection);
        }
    }
}
