package com.shu.hbase.test.fastdfs;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FastDFSClient {
    private static Logger logger = LoggerFactory.getLogger(FastDFSClient.class);
    private static StorageClient1 storageClient1 = null;

    private static FastDFSConfig fastDFSConfig = new FastDFSConfig();

    static {
        try {
            // 获取配置文件
            //String classPath = new File(FastDFSClient.class.getResource("/").getFile()).getCanonicalPath();
            // String CONF_FILENAME = classPath + File.separator + "conf" + File.separator + "fdfs_client.conf";
            //ClientGlobal.init(CONF_FILENAME);
            logger.info("初始化开始");
            initClientGlobal();
            logger.info("初始化成功");
            // 获取触发器
            TrackerClient trackerClient = new TrackerClient(ClientGlobal.g_tracker_group);
            TrackerServer trackerServer = trackerClient.getConnection();
            // 获取存储服务器
            StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);
            storageClient1 = new StorageClient1(trackerServer, storageServer);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void initClientGlobal() throws IOException, MyException {
        ClientGlobal.initByTrackers(fastDFSConfig.getTrackerServer());

        if (fastDFSConfig.getTrackerHttpPort() != null && fastDFSConfig.getTrackerHttpPort().intValue() != 0) {
            ClientGlobal.setG_tracker_http_port(fastDFSConfig.getTrackerHttpPort());
        }

        if (fastDFSConfig.getConnectTimeout() != null && fastDFSConfig.getConnectTimeout().intValue() != 0) {
            ClientGlobal.setG_connect_timeout(fastDFSConfig.getConnectTimeout());
        }
        if (fastDFSConfig.getNetworkTimeout() != null && fastDFSConfig.getNetworkTimeout().intValue() != 0) {
            ClientGlobal.setG_network_timeout(fastDFSConfig.getNetworkTimeout());
        }
    }


    /**
     * @param fis      文件输入流
     * @param fileName 文件名称
     * @return
     */
    public static String uploadFile(InputStream fis, String fileName) {
        try {
            NameValuePair[] meta_list = null;

            //将输入流写入file_buff数组
            byte[] file_buff = null;
            if (fis != null) {
                int len = fis.available();
                file_buff = new byte[len];
                fis.read(file_buff);
            }
            String fileid = storageClient1.upload_file1(file_buff, getFileExt(fileName), meta_list);
            return fileid;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }


    private static String getFileExt(String fileName) {
        if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
            return "";
        } else {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
    }
}

