package com.xunfang.healthcare.common;

import lombok.Getter;

/**
 * 业务异常，由全局异常处理器转换为统一响应结构。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(String message) {
        return new BizException(message);
    }

    public static BizException badRequest(String message) {
        return new BizException(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ResultCode.FORBIDDEN.getCode(), message);
    }
}
