package com.shu.hbase.test;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;

@RestController
@CrossOrigin
public class UrlTestController {

    private Logger logger = LoggerFactory.getLogger(UrlTestController.class);

    @GetMapping("testUrl")
    public void testUrl() {
        try {
            logger.info("注册fdfs");
            URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());//注册hdfs的URL
            logger.info("开始获得文件输入流");
            InputStream inputStream = new URL("hdfs://bdg-proto-nameservice/shuwebfs/00000000/上海大学奖学金评选申请0.docx").openStream();//获取文件输入流
            logger.info("成功获得inputStream");
            FileOutputStream outputStream = new FileOutputStream(new File("/home/file1_out.txt"));
            logger.info("打开outputStream");
            logger.info("开始拷贝");
            IOUtils.copy(inputStream, outputStream);//实现文件的拷贝
            IOUtils.closeQuietly(inputStream);//关闭流
            IOUtils.closeQuietly(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
