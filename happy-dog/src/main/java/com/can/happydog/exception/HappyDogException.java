package com.can.happydog.exception;

/**
 * 快乐小狗自定义异常
 */
public class HappyDogException extends RuntimeException {
    
    private String errorCode;
    
    public HappyDogException(String message) {
        super(message);
    }
    
    public HappyDogException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public HappyDogException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public HappyDogException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}