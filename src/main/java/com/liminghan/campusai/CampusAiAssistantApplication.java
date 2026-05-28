package com.liminghan.campusai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.liminghan.campusai.mapper")
@SpringBootApplication
public class CampusAiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAiAssistantApplication.class, args);
    }
}
