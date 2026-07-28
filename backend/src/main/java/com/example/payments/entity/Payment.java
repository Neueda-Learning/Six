// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 支付主表对应的实体对象。
 * 该实体用于表示系统中一笔支付的当前快照信息，包含幂等键、账户信息、金额、币种、当前状态、
 * 错误信息以及创建和更新时间等字段。
 * 在业务上，它代表支付生命周期中的“当前状态视图”，通常与历史表配合使用。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("payments")
public class Payment {
    @TableId
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

    // 乐观锁版本号：配合 MybatisPlusConfig 中注册的乐观锁插件，updateById 时自动附加版本号条件并递增，防止并发覆盖
    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime permanentlyDeletedAt;

    // todo add MyBatis-Plus annotations for id/version and generate getters/setters
}