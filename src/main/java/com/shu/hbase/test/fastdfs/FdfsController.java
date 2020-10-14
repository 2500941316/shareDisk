package com.shu.hbase.test.fastdfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@CrossOrigin
public class FdfsController {

    @Autowired
    FastDFSClient fastDFSClient;

    @GetMapping("testFdfs")
    public void testFdfs() {
        FastDFSFileMeta fastDFSFileMeta = new FastDFSFileMeta();
        fastDFSFileMeta.setAuthor("cxy");
        fastDFSFileMeta.setContent(new byte[9]);
        fastDFSClient.upload(fastDFSFileMeta);
    }
}
