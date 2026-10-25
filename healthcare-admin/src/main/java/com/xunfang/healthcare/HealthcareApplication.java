package com.xunfang.healthcare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 医疗后台管理系统启动类。
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.xunfang.healthcare.mapper")
@ConfigurationPropertiesScan("com.xunfang.healthcare.config.properties")
@SpringBootApplication
public class HealthcareApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthcareApplication.class, args);
    }
}
