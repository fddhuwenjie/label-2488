package com.library.common;

import lombok.Getter;

/**
 * 统一 API 响应结果封装类
 *
 * <p>所有接口统一使用此类封装返回值，包含状态码、消息和业务数据。
 * 通过静态工厂方法创建实例，禁止外部直接构造。
 *
 * <p>约定状态码：
 * <ul>
 *   <li>200：操作成功</li>
 *   <li>400：请求参数错误 / 业务逻辑错误</li>
 *   <li>401：未认证（未登录或 Token 失效）</li>
 *   <li>403：权限不足</li>
 *   <li>500：服务器内部错误</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 */
@Getter
public class Result<T> {

    /** HTTP 业务状态码 */
    private final int code;

    /** 响应消息（成功提示或错误描述） */
    private final String message;

    /** 业务数据（失败时为 null） */
    private final T data;

    /** 私有构造，统一通过静态工厂方法创建 */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功响应，携带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 成功响应，携带自定义消息和数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /** 成功响应，无数据（用于删除等操作） */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /** 失败响应（默认 500 内部错误） */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /** 失败响应，携带自定义状态码 */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
