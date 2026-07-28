// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建支付请求 DTO。
 * 该对象用于接收客户端在创建支付时提交的请求体数据，包含幂等键、付款账户、收款账户、金额、
 * 币种和备注等字段。
 * 它位于接口层和服务层之间，主要承担参数传输与基础校验注解承载的职责。
 */
@Data
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
}