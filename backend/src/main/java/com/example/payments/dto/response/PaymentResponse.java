// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

/**
 * 支付详情响应 DTO。
 * 该对象用于向前端返回一笔支付当前的完整展示信息，通常包括主键、幂等键、账户信息、金额、币种、
 * 当前状态、错误信息以及创建和更新时间等字段。
 * 它通常由实体对象转换而来，是详情页和列表展示的重要数据载体。
 */
@Data
public class PaymentResponse {

    // 主键使用雪花算法生成（19位长整型），超出 JavaScript 安全整数范围（2^53-1），
    // 序列化为字符串返回，避免前端 JSON 解析时发生精度丢失导致查询不到该支付记录
    @JsonSerialize(using = ToStringSerializer.class)
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

    // todo generate getters/setters
}