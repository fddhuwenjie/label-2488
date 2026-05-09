package com.library.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 图书创建 / 更新请求 DTO
 * 管理员通过此 DTO 新增或修改图书信息，包含字段合法性校验
 */
@Data
public class BookRequest {

    /** ISBN 编号（可选，不超过 20 位） */
    @Size(max = 20, message = "ISBN 长度不能超过 20 个字符")
    private String isbn;

    /** 图书标题，必填 */
    @NotBlank(message = "图书标题不能为空")
    @Size(max = 200, message = "图书标题不能超过 200 个字符")
    private String title;

    /** 作者，必填 */
    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者名不能超过 100 个字符")
    private String author;

    /** 出版社（可选） */
    @Size(max = 100, message = "出版社名称不能超过 100 个字符")
    private String publisher;

    /** 图书分类（可选，如：计算机、文学、历史） */
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String category;

    /** 馆藏总数量，必填，范围 1-9999 */
    @NotNull(message = "馆藏数量不能为空")
    @Min(value = 1, message = "馆藏数量至少为 1")
    @Max(value = 9999, message = "馆藏数量不能超过 9999")
    private Integer totalStock;

    /** 图书简介（可选，不超过 1000 字） */
    @Size(max = 1000, message = "图书简介不能超过 1000 个字符")
    private String description;
}
