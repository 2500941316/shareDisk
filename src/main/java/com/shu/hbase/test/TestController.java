package com.shu.hbase.test;

import com.shu.hbase.tools.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class TestController {

    private Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("testPublicFile")
    public String testPublicFile() {


        for (int i = 0; i < 15; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("新的线程启动了"+Thread.currentThread().getName());
                    final String url = "http://localhost:8080/getPublicFiles";
                    String string = Get.sendGet(url, "", "");
                    System.out.println(string);
                }
            }).start();
        }

        return "success";
    }


    @GetMapping("searchFile")
    public String downLoadTest() {
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("新的线程启动了"+Thread.currentThread().getName());
                    final String url = "http://localhost:8080/searchFile";
                    String string = Get.sendGet(url, "value=3&type=share", "");
                    System.out.println(string);
                }
            }).start();
        }

        return "success";
    }

}
