// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.statemachine;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.payments.enums.PaymentStatus;

@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, Set.of(PaymentStatus.VALIDATED, PaymentStatus.FAILED),
            PaymentStatus.VALIDATED, Set.of(PaymentStatus.SENT, PaymentStatus.FAILED),
            PaymentStatus.SENT, Set.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED),
            PaymentStatus.COMPLETED, Set.of(),
            PaymentStatus.FAILED, Set.of()
    );

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        //todo replace boolean return with domain exception if needed
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
