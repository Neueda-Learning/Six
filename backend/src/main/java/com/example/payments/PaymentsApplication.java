// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentsApplication {

    public static void main(String[] args) {
        //todo boot application entry point
        SpringApplication.run(PaymentsApplication.class, args);
    }
}
