package com.tradelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.io.File;

@SpringBootApplication
@EnableCaching
public class TradeLogApplication {

    public static void main(String[] args) {
        new File(System.getProperty("user.home") + "/tradelog").mkdirs();
        SpringApplication.run(TradeLogApplication.class, args);
    }
}
