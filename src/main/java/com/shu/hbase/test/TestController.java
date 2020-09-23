package com.shu.hbase.test;

import com.shu.hbase.tools.Get;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class TestController {
    @GetMapping("testPublicFile")
    public String testPublicFile() {
        String url = "http://localhost:8080/getPublicFiles";

        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String string = Get.sendGet(url, "", "");
                    System.out.println(string);
                }
            }).start();
        }

        return "success";
    }

}
