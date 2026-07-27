// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("payment_status_history")
public class PaymentStatusHistory {

    private Long id;
    private Long paymentId;
    private String fromStatus;
    private String toStatus;
    private String errorCode;
    private String errorMessage;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;

    //todo add MyBatis-Plus id annotation and generate getters/setters
}