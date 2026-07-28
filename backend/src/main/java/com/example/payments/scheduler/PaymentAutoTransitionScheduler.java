package com.example.payments.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.payments.service.PaymentService;

/**
 * 定时推进支付状态，模拟真实网络处理中按固定延迟发生的异步状态演进。
 */
@Component
public class PaymentAutoTransitionScheduler {

  private final PaymentService paymentService;

  public PaymentAutoTransitionScheduler(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Scheduled(initialDelayString = "${payments.auto-transition.initial-delay-ms:5000}", fixedDelayString = "${payments.auto-transition.fixed-delay-ms:5000}")
  public void autoAdvancePendingPayments() {
    paymentService.autoAdvancePendingPayments();
  }
}