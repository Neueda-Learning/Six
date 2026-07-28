// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.enums;

/**
 * 业务错误码枚举。
 * 该枚举集中定义支付系统中可能返回的标准错误码，便于后端统一抛错、前端统一识别，
 * 同时也有助于接口文档、日志分析和测试断言保持一致。
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    INSUFFICIENT_FUNDS,
    INVALID_ACCOUNT,
    INVALID_CURRENCY,
    INVALID_AMOUNT,
    DUPLICATE_PAYMENT,
    INVALID_STATUS_TRANSITION,
    PAYMENT_NOT_FOUND,
    RECYCLE_BIN_RECORD_NOT_FOUND,
    RESOURCE_NOT_FOUND,
    PROCESSING_ERROR,
    NETWORK_ERROR
}
