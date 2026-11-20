package com.campus.eventsys.common;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("系统繁忙，请稍后重试");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败：{}", e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 账号已被禁用（封禁）：登录或携带Token访问时账号 status=0
     */
    @ExceptionHandler(DisabledException.class)
    public Result<Void> handleDisabledException(DisabledException e) {
        log.warn("账号已被禁用：{}", e.getMessage());
        return Result.error(401, "该账号已被禁用，请联系管理员");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.error(403, "权限不足，无法访问");
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败：{}", message);
        return Result.error(400, message);
    }

    /**
     * Token过期异常
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public Result<Void> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("Token已过期：{}", e.getMessage());
        return Result.error(401, "用户信息已失效，请重新登录");
    }

    /**
     * JWT异常（Token无效、签名错误等）
     */
    @ExceptionHandler(JwtException.class)
    public Result<Void> handleJwtException(JwtException e) {
        log.warn("Token无效：{}", e.getMessage());
        return Result.error(401, "用户信息已失效，请重新登录");
    }

    /**
     * 认证信息不足（未登录）
     */
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public Result<Void> handleInsufficientAuthenticationException(InsufficientAuthenticationException e) {
        log.warn("未登录访问：{}", e.getMessage());
        return Result.error(401, "请先登录");
    }

    /**
     * 认证凭证过期
     */
    @ExceptionHandler(CredentialsExpiredException.class)
    public Result<Void> handleCredentialsExpiredException(CredentialsExpiredException e) {
        log.warn("认证凭证过期：{}", e.getMessage());
        return Result.error(401, "用户信息已失效，请重新登录");
    }

    /**
     * 通用认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证异常：{}", e.getMessage());
        return Result.error(401, "用户信息已失效，请重新登录");
    }

    /**
     * 文件大小超出限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超出限制：{}", e.getMessage());
        return Result.error(400, "文件大小超出限制，最大允许10MB");
    }

    /**
     * 静态资源未找到（如不存在的图片 /api/images/xxx.jpg、错误的前端路由路径）
     * 返回真实 HTTP 404 + 统一格式 {code:404, message, data}，
     * 避免浏览器 <img> 拿到 200 + JSON 而表现为破损/加载失败
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("静态资源不存在：{}", e.getResourcePath());
        return Result.error(404, "资源不存在");
    }
}
