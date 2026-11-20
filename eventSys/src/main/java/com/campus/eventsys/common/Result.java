package com.campus.eventsys.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应结果包装类
 * @param <T> 响应数据类型
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "响应状态码：200-成功，500-错误，401-未授权，403-无权限，404-不存在", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    public Result() {}

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 业务错误（单参）：默认业务错误码 400。
     * 仅用于 controller/service 的业务校验拒绝（如"您已报名该活动"），
     * 系统异常请走 GlobalExceptionHandler 统一返回 500。
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
