// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

import java.time.LocalDateTime;

public class PaymentHistoryItemResponse {

    private Long id;
    private Long paymentId;
    private String fromStatus;
    private String toStatus;
    private String errorCode;
    private String errorMessage;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;

    //todo generate getters/setters
}