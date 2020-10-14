package com.shu.hbase.test.fastdfs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FastDFSConfig {
    private String DEFAULT_CHARSET="UTF-8";

    @Value(value = "${fastdfs.connectTimeout}")
    private Integer connectTimeout;

    @Value(value = "${fastdfs.networkTimeout}")
    private Integer networkTimeout;

    @Value(value = "${fastdfs.trackerHttpPort}")
    private Integer trackerHttpPort;

    @Value(value = "${fastdfs.trackerServer}")
    private String trackerServer;

    public FastDFSConfig() {
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getNetworkTimeout() {
        return networkTimeout;
    }

    public void setNetworkTimeout(Integer networkTimeout) {
        this.networkTimeout = networkTimeout;
    }

    public Integer getTrackerHttpPort() {
        return trackerHttpPort;
    }

    public void setTrackerHttpPort(Integer trackerHttpPort) {
        this.trackerHttpPort = trackerHttpPort;
    }

    public String getTrackerServer() {
        return trackerServer;
    }

    public void setTrackerServer(String trackerServer) {
        this.trackerServer = trackerServer;
    }

    @Override
    public String toString() {
        return "FastDFSConfig{" +
                "connectTimeout=" + connectTimeout +
                ", networkTimeout=" + networkTimeout +
                ", trackerHttpPort=" + trackerHttpPort +
                ", trackerServer='" + trackerServer + '\'' +
                '}';
    }
}