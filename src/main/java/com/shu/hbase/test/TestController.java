package com.shu.hbase.test;

import com.shu.hbase.tools.Get;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class TestController {
    @GetMapping("testPublicFile")
    public String testPublicFile() throws Exception {
        String url = "http://localhost:8080/getPublicFiles";

        return  Get.sendGet(url, "", "");

    }

}
