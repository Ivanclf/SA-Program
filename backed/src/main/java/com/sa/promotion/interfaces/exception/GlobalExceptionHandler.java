package com.sa.promotion.interfaces.exception;

import com.sa.promotion.application.dto.response.ApiResponse;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

/**
 * 全局异常处理器
 *
 * 统一处理所有 Controller 层抛出的异常，返回标准 ApiResponse 格式。
 * Controller 方法无需再手动 try-catch。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 资源未找到 → 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.notFound(e.getMessage());
    }

    /**
     * 参数校验失败 / 业务规则不满足 → 400
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ApiResponse<Void> handleBadRequest(RuntimeException e) {
        return ApiResponse.badRequest(e.getMessage());
    }

    /**
     * 日期时间格式错误 → 400
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ApiResponse<Void> handleDateTimeParse(DateTimeParseException e) {
        return ApiResponse.badRequest("Invalid date format: " + e.getMessage());
    }

    /**
     * 未预期的异常 → 500
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleInternalError(Exception e) {
        return ApiResponse.error(500, "Internal server error: " + e.getMessage());
    }
}
