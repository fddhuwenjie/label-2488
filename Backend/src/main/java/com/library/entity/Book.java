package com.library.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书实体类，对应数据库 books 表
 *
 * <p>记录图书的基本信息和馆藏库存状态。
 * 删除操作采用逻辑删除（status=0），保留历史借阅记录的完整性。
 */
@Data
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 图书唯一标识（自增主键） */
    private Long id;

    /** ISBN 国际标准书号（可选，允许无 ISBN 的内部资料） */
    private String isbn;

    /** 图书名称 */
    private String title;

    /** 作者（多作者用逗号分隔） */
    private String author;

    /** 出版社名称 */
    private String publisher;

    /** 图书分类（如：计算机、文学、历史、数学等） */
    private String category;

    /** 馆藏总册数（管理员设置，不随借还变化） */
    private Integer totalStock;

    /**
     * 当前可借册数。
     * 借书时减 1，还书时加 1，通过数据库行级更新保证并发安全
     */
    private Integer availableStock;

    /** 图书简介（不超过 1000 字） */
    private String description;

    /** 上架状态：1 正常上架 / 0 已下架（逻辑删除） */
    private Integer status;

    /** 图书录入时间 */
    private LocalDateTime createdAt;

    /** 图书信息最后更新时间 */
    private LocalDateTime updatedAt;
}
