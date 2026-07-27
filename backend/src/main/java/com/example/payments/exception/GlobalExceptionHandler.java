// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.payments.dto.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentException(PaymentException ex) {
        //todo enrich error payload with timestamp and request path if needed
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        //todo aggregate field validation messages
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("VALIDATION_FAILED", "Request validation failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        //todo log exception details
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("PROCESSING_ERROR", "Internal processing error"));
    }
}