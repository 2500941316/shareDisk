package com.shu.hbase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    private static Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.trustStore",
                "/usr/local/springboot/krb/BDGRootCA.truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "shu");
        SpringApplication.run(DemoApplication.class, args);

        String cmd = "C:\\Users\\Administrator\\AppData\\Roaming\\360se6\\Application\\360se.exe http://localhost:8080";
        Runtime run = Runtime.getRuntime();
        try {
            run.exec(cmd);
            logger.debug("启动浏览器打开项目成功");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
