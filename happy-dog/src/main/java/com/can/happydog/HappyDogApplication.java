package com.can.happydog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 快乐小狗 主应用类
 */
@SpringBootApplication
public class HappyDogApplication {

    private static final Logger log = LoggerFactory.getLogger(HappyDogApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HappyDogApplication.class, args);
        
        // 打印启动信息
        String serverPort = context.getEnvironment().getProperty("server.port", "8081");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
        
        log.info("===========================================");
        log.info("🐕 快乐小狗 启动成功！");
        log.info("访问地址: http://localhost:" + serverPort + contextPath);
        log.info("健康检查: http://localhost:" + serverPort + contextPath + "/api/ai/health");
        log.info("===========================================");
    }
}