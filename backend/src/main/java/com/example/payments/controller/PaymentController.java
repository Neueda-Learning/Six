//该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.payments.dto.request.CreatePaymentRequest;
import com.example.payments.dto.request.UpdatePaymentStatusRequest;
import com.example.payments.dto.response.ApiResponse;
import com.example.payments.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<Object> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        //todo call paymentService.createPayment(request)
        return ApiResponse.todo();
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> getPaymentById(@PathVariable Long id) {
        //todo call paymentService.getPaymentById(id)
        return ApiResponse.todo();
    }

    @GetMapping("/{id}/history")
    public ApiResponse<Object> getPaymentHistory(@PathVariable Long id) {
        //todo call paymentService.getPaymentHistory(id)
        return ApiResponse.todo();
    }

    @GetMapping
    public ApiResponse<Object> listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        //todo call paymentService.listPayments(status, keyword, page, size)
        return ApiResponse.todo();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Object> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        //todo call paymentService.updatePaymentStatus(id, request)
        return ApiResponse.todo();
    }
}

