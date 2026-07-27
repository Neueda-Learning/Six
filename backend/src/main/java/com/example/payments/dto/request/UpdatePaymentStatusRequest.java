// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 手动更新支付状态请求 DTO。
 * 该对象用于接收状态流转测试或人工干预场景下提交的请求参数，
 * 包括目标状态以及可选的错误码、错误信息和备注。
 */
public class UpdatePaymentStatusRequest {

    @NotBlank
    private String targetStatus;

    private String errorCode;

    private String errorMessage;

    private String remark;

    // todo generate getters/setters or replace with Lombok @Data
}