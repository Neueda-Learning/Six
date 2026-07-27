// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.statemachine;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.payments.enums.PaymentStatus;

/**
 * 支付状态机规则组件。
 * 该类集中维护支付状态之间允许发生的流转关系，避免状态跳转逻辑散落在多个业务方法中。
 * 服务层在执行状态更新前，应该通过该组件先判断从当前状态到目标状态的转换是否合法。
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, Set.of(PaymentStatus.VALIDATED, PaymentStatus.FAILED),
            PaymentStatus.VALIDATED, Set.of(PaymentStatus.SENT, PaymentStatus.FAILED),
            PaymentStatus.SENT, Set.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED),
            PaymentStatus.COMPLETED, Set.of(),
            PaymentStatus.FAILED, Set.of());

    /**
     * 判断支付状态是否允许从当前状态流转到目标状态。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 如果允许流转则返回 true，否则返回 false
     */
    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        // todo replace boolean return with domain exception if needed
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
