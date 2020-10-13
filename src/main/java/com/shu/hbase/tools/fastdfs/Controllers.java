package com.shu.hbase.tools.fastdfs;

import org.apache.http.NameValuePair;

import org.csource.common.MyException;
import org.csource.fastdfs.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class Controllers {
    private static final Logger logger = LoggerFactory.getLogger(Controllers.class);

    @Autowired
    private StorageClientConfig storageClient;

    @GetMapping("testFdfs")
    public void test() throws IOException, MyException {
        StorageClient storageClient = this.storageClient.storageClient();

    }

}

