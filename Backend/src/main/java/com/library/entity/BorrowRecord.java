package com.library.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体类，对应数据库 borrow_records 表
 *
 * <p>记录每次图书借阅的完整生命周期（借出→在借→归还/逾期）。
 *
 * <p><b>MyBatis 延迟加载说明：</b>
 * {@link #user} 和 {@link #book} 字段通过 MyBatis 的 {@code fetchType="lazy"} 配置实现延迟加载。
 * 仅在代码显式调用 {@code getUser()} 或 {@code getBook()} 时，才会触发额外的 SQL 查询，
 * 避免不必要的数据库访问，提升列表查询的性能。
 */
@Data
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 借阅记录唯一标识（自增主键） */
    private Long id;

    /** 借阅用户 ID（外键关联 users 表） */
    private Long userId;

    /** 图书 ID（外键关联 books 表） */
    private Long bookId;

    /** 借阅日期 */
    private LocalDate borrowDate;

    /** 应还日期（借阅日期 + 30 天） */
    private LocalDate dueDate;

    /** 实际归还日期（未归还时为 null） */
    private LocalDate returnDate;

    /**
     * 借阅状态：
     * <ul>
     *   <li>{@code BORROWED}：借阅中（未超期）</li>
     *   <li>{@code RETURNED}：已归还</li>
     *   <li>{@code OVERDUE}：已逾期（超过应还日期未归还）</li>
     * </ul>
     */
    private String status;

    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /**
     * 关联用户信息（MyBatis 延迟加载）。
     * 通过 BorrowMapper.xml 中的 association + select 实现按需加载
     */
    private User user;

    /**
     * 关联图书信息（MyBatis 延迟加载）。
     * 通过 BorrowMapper.xml 中的 association + select 实现按需加载
     */
    private Book book;
}
