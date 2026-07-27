// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreatePaymentRequest {

    @NotBlank
    @Size(max = 64)
    private String idempotencyKey;

    @NotBlank
    private String fromAccount;

    @NotBlank
    private String toAccount;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    private String remark;

    //todo generate getters/setters or replace with Lombok @Data
}