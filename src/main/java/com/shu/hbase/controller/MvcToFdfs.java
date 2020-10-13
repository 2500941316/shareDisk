package com.shu.hbase.controller;

import com.shu.hbase.tools.fastdfs.FastDFSClient;
import io.swagger.annotations.ApiOperation;
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

    @GetMapping("testFdfs")
    public String testFdfs() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("C:\\Users\\Administrator\\Pictures\\test.jpg");

        String fileID = FastDFSClient.uploadFile(inputStream, "test.jpg");
        System.out.println(fileID);
        return fileID;
    }


}
