// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.enums;

/**
 * 支付生命周期状态枚举。
 * 该枚举定义一笔支付从创建到结束过程中可能出现的所有阶段，
 * 供状态机、服务层、持久化层和接口响应共同使用，以保持状态语义一致。
 */
public enum PaymentStatus {
    CREATED,
    VALIDATED,
    SENT,
    COMPLETED,
    FAILED
}