package com.shu.hbase.test;

import com.shu.hbase.tools.hdfspool.HdfsConnectionPool;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;

@RestController
@CrossOrigin
public class UrlTestController {

    private Logger logger = LoggerFactory.getLogger(UrlTestController.class);

    @GetMapping("testUrl")
    public void testUrl() {
        try {
            String target = "/shuwebfs/00000000/上海大学奖学金评选申请0.docx";//hdfs文件 地址
            FileSystem hdfsConnection = HdfsConnectionPool.getHdfsConnection();
            logger.info("获取到fs连接");
            hdfsConnection.copyToLocalFile(new Path(target), new Path("/home/testHdfs.docx"));
            logger.info("文件拷贝成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
