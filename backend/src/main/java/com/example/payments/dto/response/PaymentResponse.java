// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long id;
    private String idempotencyKey;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String errorCode;
    private String errorMessage;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //todo generate getters/setters
}