// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 后端应用启动入口。
 * 该类负责装配 Spring Boot 上下文，并在程序启动时完成组件扫描、配置加载、Web 容器初始化等基础工作。
 * 对本项目来说，它是支付处理系统后端服务的统一入口，通常不承载具体业务逻辑。
 */
@SpringBootApplication
@EnableScheduling
public class PaymentsApplication {

    public static void main(String[] args) {
        // todo boot application entry point
        SpringApplication.run(PaymentsApplication.class, args);
    }
}
