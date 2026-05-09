package com.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 借阅图书请求 DTO
 */
@Data
public class BorrowRequest {

    /** 要借阅的图书 ID，必填 */
    @NotNull(message = "图书 ID 不能为空")
    private Long bookId;
}
