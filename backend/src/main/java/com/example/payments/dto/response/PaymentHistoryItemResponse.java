// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

/**
 * 支付历史记录项响应 DTO。
 * 该对象表示支付状态时间线中的单条记录，主要用于展示某次状态变更的前后状态、错误信息、
 * 操作来源和发生时间。
 * 多条该对象通常会组合成一个历史列表返回给前端。
 */
@Data
public class PaymentHistoryItemResponse {

    // 主键与关联支付ID均为雪花算法生成的长整型，序列化为字符串避免前端精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long paymentId;
    private String fromStatus;
    private String toStatus;
    private String errorCode;
    private String errorMessage;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;

    // todo generate getters/setters
}