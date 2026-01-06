package com.yupi.springbootinit.exception;

public abstract class BaseException extends RuntimeException {

    private final int code;

    protected BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
