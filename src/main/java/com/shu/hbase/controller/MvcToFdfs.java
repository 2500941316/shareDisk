package com.shu.hbase.controller;

import com.shu.hbase.tools.fastdfs.FastDFSClient;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RestController
@CrossOrigin
public class MvcToFdfs {

    private static Logger logger = LoggerFactory.getLogger(MvcToFdfs.class);

    @GetMapping("testFdfs")
    public static String testFdfs() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("/etc/fdfs/test.jpg");
        logger.info("开始执行上传逻辑");
        String fileID = FastDFSClient.uploadFile(inputStream, "test.jpg");
        System.out.println(fileID);
        return fileID;
    }




}
