// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.validator;

import org.springframework.stereotype.Component;

import com.example.payments.dto.request.CreatePaymentRequest;

@Component
public class PaymentValidator {

    public void validateCreateRequest(CreatePaymentRequest request) {
        //todo implement amount/account/currency checks based on course rules
    }
}
