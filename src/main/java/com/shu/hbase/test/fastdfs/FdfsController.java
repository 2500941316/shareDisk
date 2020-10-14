package com.shu.hbase.test.fastdfs;

import com.shu.hbase.service.impl.upload.MvcToFastDfs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@RestController
@CrossOrigin
public class FdfsController {

    @Autowired
    MvcToFastDfs mvcToFastDfs;

    @GetMapping("testFastdfs")
    public void testFastdfs() throws FileNotFoundException {
        String string = mvcToFastDfs.uploadFile(new FileInputStream("/usr/local/springboot/test.txt"), "test.txt");
        System.out.println(string);
    }
}
