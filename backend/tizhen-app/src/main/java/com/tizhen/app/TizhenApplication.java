package com.tizhen.app;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 题阵对外 Web 进程：REST API、WebSocket、认证与会话。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.tizhen")
@MapperScan(basePackages = "com.tizhen.core", annotationClass = Mapper.class)
public class TizhenApplication {

    public static void main(String[] args) {
        SpringApplication.run(TizhenApplication.class, args);
    }
}
