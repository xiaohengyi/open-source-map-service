package com.yupi.springbootinit.exception;

import com.yupi.springbootinit.common.ErrorCode;

public class BusinessException extends BaseException {

    public BusinessException(int code, String message) {
        super(code, message);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
