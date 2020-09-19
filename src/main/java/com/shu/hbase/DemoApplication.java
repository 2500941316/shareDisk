package com.shu.hbase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.trustStore",
                "/usr/local/springboot/krb/BDGRootCA.truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "shu");
        SpringApplication.run(DemoApplication.class, args);
    }
}
