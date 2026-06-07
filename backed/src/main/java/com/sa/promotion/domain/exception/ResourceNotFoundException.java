package com.sa.promotion.domain.exception;

/**
 * 资源未找到异常
 * 用于全局异常处理器区分"未找到"(404)和"参数错误"(400)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
