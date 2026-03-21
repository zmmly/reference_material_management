package com.rmm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.rmm.mapper")
public class RmmApplication {
    public static void main(String[] args) {
        SpringApplication.run(RmmApplication.class, args);
    }
}
