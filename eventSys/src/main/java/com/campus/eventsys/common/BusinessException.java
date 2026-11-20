package com.campus.eventsys.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 业务异常单参构造：默认业务错误码 400（与 CLAUDE.md 约定一致），
     * 供业务校验拒绝使用；系统级异常不应使用本类，交由 GlobalExceptionHandler 返回 500
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
