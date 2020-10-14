package com.shu.hbase.test.fastdfs;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@RestController
@CrossOrigin
public class FdfsController {

    @GetMapping("testFdfs")
    public void testFdfs() throws FileNotFoundException {
        //FastDFSClient.uploadFile(new FileInputStream(new File("C:\\Users\\Administrator\\Desktop\\test.txt")),"test.txt");
        FastDFSClient.uploadFile(new FileInputStream(new File("/usr/local/springboot/test.txt")), "test.txt");
    }
}
