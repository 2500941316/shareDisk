package com.shu.hbase.tools.fastdfs;

import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StorageClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(StorageClientConfig.class);

    private FastDFSConfig fastDFSConfig;

    public StorageClient storageClient() throws IOException, MyException {
        logger.info("开始初始化");
        this.initClientGlobal();
        logger.info("初始化成功");
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getConnection();
        StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);

        return new StorageClient(trackerServer, storageServer);
    }

    private void initClientGlobal() throws IOException, MyException {
        ClientGlobal.initByTrackers(fastDFSConfig.getTrackerServer());

        if(fastDFSConfig.getTrackerHttpPort()!=null && fastDFSConfig.getTrackerHttpPort().intValue()!=0) {
            ClientGlobal.setG_tracker_http_port(fastDFSConfig.getTrackerHttpPort());
        }

        if(fastDFSConfig.getConnectTimeout()!=null && fastDFSConfig.getConnectTimeout().intValue()!=0) {
            ClientGlobal.setG_connect_timeout(fastDFSConfig.getConnectTimeout());
        }
        if(fastDFSConfig.getNetworkTimeout()!=null && fastDFSConfig.getNetworkTimeout().intValue()!=0) {
            ClientGlobal.setG_network_timeout(fastDFSConfig.getNetworkTimeout());
        }
    }

}