// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.exception;

import org.springframework.http.HttpStatus;

/**
 * 支付领域自定义异常。
 * 该异常用于封装业务错误码、错误消息以及对应的 HTTP 状态码，
 * 让服务层在发现业务问题时可以抛出统一模型，再由全局异常处理器转换成标准接口响应。
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public PaymentException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
