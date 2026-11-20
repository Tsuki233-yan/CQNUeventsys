package com.campus.eventsys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campus.eventsys.mapper")
@EnableScheduling
public class EventSysApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventSysApplication.class, args);
    }
}
