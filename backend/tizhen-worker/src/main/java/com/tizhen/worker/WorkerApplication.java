package com.tizhen.worker;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 题阵异步任务进程：判题消费、造数据、CF 同步与文件清理。
 * 不对外提供 HTTP 服务，仅使用 Spring 调度与数据库任务表。
 */
@SpringBootApplication(scanBasePackages = "com.tizhen")
@MapperScan(basePackages = "com.tizhen.core", annotationClass = Mapper.class)
@EnableScheduling
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
