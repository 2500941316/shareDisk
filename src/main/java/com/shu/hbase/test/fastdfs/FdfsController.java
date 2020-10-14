package com.shu.hbase.test.fastdfs;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@RestController
@CrossOrigin
public class FdfsController {

    @GetMapping("testFastdfs")
    public void testFastdfs() throws FileNotFoundException {
       FastDFSClient.uploadFile(new FileInputStream("/usr/local/springboot/test.txt"), "test.txt");
        // FastDFSClient.uploadFile(new FileInputStream("C:\\Users\\Administrator\\Desktop\\test.txt"), "test.txt");
    }
}
