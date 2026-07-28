package com.example.payments.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.payments.enums.PaymentStatus;

/**
 * 覆盖 test-cases.md 第六章状态流转用例 TC-24 ~ TC-32。
 * 用一个参数化测试遍历 PaymentStatus 全部 5x5=25 种流转组合，
 * 逐一比对 PaymentStateMachine 的判定结果是否与设计规则一致：
 * CREATED->VALIDATED, CREATED->FAILED, VALIDATED->SENT, VALIDATED->FAILED, SENT->COMPLETED, SENT->FAILED 合法，
 * 其余组合（含任何跳级、逆向流转，以及 COMPLETED/FAILED 两个终态发起的流转）均非法。
 */
class PaymentStateMachineTest {

    private static final Set<String> LEGAL_TRANSITIONS = Set.of(
            "CREATED->VALIDATED",
            "CREATED->FAILED",
            "VALIDATED->SENT",
            "VALIDATED->FAILED",
            "SENT->COMPLETED",
            "SENT->FAILED");

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    // TC-24~TC-32：合法流转返回 true，非法流转（跳级/逆向/终态再流转）返回 false
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allTransitionPairs")
    void canTransition_matchesDesignedRules(PaymentStatus from, PaymentStatus to) {
        boolean expected = LEGAL_TRANSITIONS.contains(from.name() + "->" + to.name());
        assertEquals(expected, stateMachine.canTransition(from, to));
    }

    static Stream<Arguments> allTransitionPairs() {
        List<Arguments> pairs = new ArrayList<>();
        for (PaymentStatus from : PaymentStatus.values()) {
            for (PaymentStatus to : PaymentStatus.values()) {
                pairs.add(Arguments.of(from, to));
            }
        }
        return pairs.stream();
    }
}
