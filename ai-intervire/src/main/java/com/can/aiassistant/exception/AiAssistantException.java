package com.can.aiassistant.exception;

/**
 * AI助手自定义异常
 */
public class AiAssistantException extends RuntimeException {
    
    private String errorCode;
    
    public AiAssistantException(String message) {
        super(message);
    }
    
    public AiAssistantException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AiAssistantException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AiAssistantException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}