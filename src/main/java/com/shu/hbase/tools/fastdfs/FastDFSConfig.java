package com.shu.hbase.tools.fastdfs;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class FastDFSConfig {
    private String DEFAULT_CHARSET = "UTF-8";

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


}