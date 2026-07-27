// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UpdatePaymentStatusRequest {

    @NotBlank
    private String targetStatus;

    private String errorCode;

    private String errorMessage;

    private String remark;

    //todo generate getters/setters or replace with Lombok @Data
}