package com.library.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一捕获并处理各类异常，将异常转换为标准 {@link Result} 格式的 JSON 响应。
 * 通过 {@link RestControllerAdvice} 注解生效，覆盖所有 {@code @RestController}。
 *
 * <p>异常处理优先级（从高到低）：
 * <ol>
 *   <li>请求参数校验失败（400）</li>
 *   <li>认证失败（401）</li>
 *   <li>权限不足（403）</li>
 *   <li>业务逻辑异常（400）</li>
 *   <li>未捕获的系统异常（500，兜底）</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 Bean Validation 请求参数校验失败。
     * 提取所有字段的错误消息并拼接后返回，HTTP 状态码 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        List<String> errors = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        String message = String.join("; ", errors);
        log.warn("请求参数校验失败: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理用户名或密码错误的认证失败异常，HTTP 状态码 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 处理账号被禁用的异常，HTTP 状态码 401
     */
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleDisabledException(DisabledException ex) {
        log.warn("账号已禁用: {}", ex.getMessage());
        return Result.error(401, "账号已被禁用，请联系管理员");
    }

    /**
     * 处理 Spring Security 权限不足异常，HTTP 状态码 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return Result.error(403, "权限不足，无法执行此操作");
    }

    /**
     * 处理业务逻辑异常（各 Service 抛出的 RuntimeException），HTTP 状态码 400
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "操作失败：" + ex.getClass().getSimpleName();
            log.error("业务异常（无消息）: ", ex);
        } else {
            log.error("业务异常: {}", message);
        }
        return Result.error(400, message);
    }

    /**
     * 兜底异常处理，捕获所有未被上述处理器捕获的异常，HTTP 状态码 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常: ", ex);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
