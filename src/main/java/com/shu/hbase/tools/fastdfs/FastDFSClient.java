package com.shu.hbase.tools.fastdfs;

import com.shu.hbase.controller.MvcToFdfs;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FastDFSClient {
    private static StorageClient1 storageClient1 = null;
    private static Logger logger = LoggerFactory.getLogger(MvcToFdfs.class);

    static {
        try {
            // 获取配置文件
            logger.info("获取配置文件根路径");
            // String classPath = new File(FastDFSClient.class.getResource("/").getFile()).getCanonicalPath();
            logger.info("获取配置文件根路径");
            //String CONF_FILENAME = classPath + File.separator + "conf" + File.separator + "fdfs_client.conf";
            String CONF_FILENAME = "/usr/local/springboot/fdfs_client.conf";
            System.out.println(CONF_FILENAME);
            ClientGlobal.init(CONF_FILENAME);
            logger.info("初始化成功！");
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

