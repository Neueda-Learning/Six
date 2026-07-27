// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 支付状态历史表对应的实体对象。
 * 该实体用于记录支付在生命周期中的每一次状态变化，包括变更前状态、变更后状态、错误信息、
 * 操作来源以及记录时间。
 * 它主要服务于审计追踪、问题排查和前端时间线展示。
 */
@Data
@TableName("payment_status_history")
public class PaymentStatusHistory {

    @TableId
    private Long id;
    private Long paymentId;
    private String fromStatus;
    private String toStatus;
    private String errorCode;
    private String errorMessage;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;

    // todo add MyBatis-Plus id annotation and generate getters/setters
}