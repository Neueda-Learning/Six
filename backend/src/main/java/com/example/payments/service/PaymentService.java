// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.service;

import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.dto.request.UpdatePaymentStatusRequest;

public interface PaymentService {

    Object createPayment(CreatePaymentRequest request);

    Object getPaymentById(Long id);

    Object getPaymentHistory(Long id);

    Object listPayments(String status, String keyword, Integer page, Integer size);

    Object updatePaymentStatus(Long id, UpdatePaymentStatusRequest request);
}
