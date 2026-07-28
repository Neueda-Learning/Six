// 该文件用于定义后端模块骨架，后续需完成对应业务逻辑、数据结构与接口实现。
package com.example.payments.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.payments.dto.response.ApiResponse;
import com.example.payments.enums.ErrorCode;

/**
 * 全局异常处理器。
 * 该类负责拦截控制器层和服务层抛出的异常，并将不同类型的异常统一转换成约定好的 API 响应格式。
 * 这样可以避免每个接口重复编写错误处理逻辑，并保证错误返回结构对前端保持一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    /**
     * 处理支付领域异常，并按异常中携带的 HTTP 状态码返回结果。
     *
     * @param ex 支付领域异常
     * @return 标准化错误响应
     */
    public ResponseEntity<ApiResponse<Object>> handlePaymentException(PaymentException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * 处理请求参数校验失败异常（如 @NotBlank、@DecimalMin 等注解校验不通过）。
     * 将所有字段级错误信息聚合为一条可读的消息，方便前端定位具体是哪个字段不满足要求，
     * 而不是返回一句固定不变的提示文案。
     *
     * @param ex 参数校验异常
     * @return 标准化错误响应
     */
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // 拼接每个字段具体的错误描述，例如 "amount: 必须大于或等于 0.01"，多个字段用分号分隔
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String finalMessage = detail.isEmpty() ? "参数校验失败" : detail;
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED.name(), finalMessage));
    }

    @ExceptionHandler(Exception.class)
    /**
     * 处理未被其他处理器捕获的通用异常。
     *
     * @param ex 通用异常
     * @return 标准化错误响应
     */
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ErrorCode.PROCESSING_ERROR.name(), "服务器内部处理异常: " + ex.getMessage()));
    }
}
