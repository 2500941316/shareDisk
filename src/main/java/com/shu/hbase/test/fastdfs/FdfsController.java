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
        String string = FastDFSClient.uploadFile(new FileInputStream("/usr/local/springboot/test.txt"), "test.txt");
        System.out.println(string);
    }
}
