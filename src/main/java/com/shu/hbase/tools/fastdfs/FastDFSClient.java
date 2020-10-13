package com.shu.hbase.tools.fastdfs;

import org.apache.http.NameValuePair;
import org.csource.fastdfs.*;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FastDFSClient {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(FastDFSClient.class);
    static TrackerClient trackerClient = null;
    static TrackerServer trackerServer = null;
    static StorageServer storageServer = null;

    static {
        try {
            logger.info("获取配置文件");
            String filePath = new ClassPathResource("fdfs_client.conf").getFile().getAbsolutePath();
            logger.info("开始初始化");
            ClientGlobal.init(filePath);
            logger.info("初始化成功！");
            trackerClient = new TrackerClient();
            logger.info("获取连接！");
            trackerServer = trackerClient.getConnection();
            storageServer = trackerClient.getStoreStorage(trackerServer);
        } catch (Exception e) {
            logger.error("FastDFS Client Init Fail!", e);
        }
    }

    public static void test() {
        System.out.println("111");
    }
}