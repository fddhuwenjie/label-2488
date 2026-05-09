package com.library.service;

import com.library.common.PageResult;
import com.library.entity.BorrowRecord;

/**
 * 借阅业务服务接口
 * 定义图书借还及借阅记录查询的核心业务操作
 */
public interface BorrowService {

    /**
     * 借阅图书
     * 校验借阅上限（每人最多5本），验证图书库存，事务性地减少库存并创建借阅记录
     *
     * @param userId 借阅用户 ID
     * @param bookId 目标图书 ID
     * @return 创建的借阅记录（含延迟加载触发后的用户和图书信息）
     * @throws RuntimeException 超出借阅上限、图书不存在或库存不足时抛出
     */
    BorrowRecord borrowBook(Long userId, Long bookId);

    /**
     * 归还图书
     * 验证记录存在性与归属权，更新状态为 RETURNED，归还图书库存
     *
     * @param recordId 借阅记录 ID
     * @param userId   当前用户 ID（用于归属权验证）
     * @return 更新后的借阅记录
     * @throws RuntimeException 记录不存在、无归还权限或状态不合法时抛出
     */
    BorrowRecord returnBook(Long recordId, Long userId);

    /**
     * 分页查询借阅记录
     *
     * @param userId   用户 ID，为 null 时查询所有用户记录（管理员权限）
     * @param status   借阅状态过滤（BORROWED/RETURNED/OVERDUE），null 表示不过滤
     * @param pageNum  页码
     * @param pageSize 每页记录数
     * @return 分页借阅记录列表（使用简单 ResultMap，不含关联对象）
     */
    PageResult<BorrowRecord> getBorrowRecords(Long userId, String status, int pageNum, int pageSize);

    /**
     * 根据 ID 查询借阅记录详情（触发 MyBatis 延迟加载）
     * 在事务范围内访问 user 和 book 属性，触发延迟加载并完整填充实体
     *
     * @param id 借阅记录 ID
     * @return 包含完整用户和图书信息的借阅记录
     * @throws RuntimeException 记录不存在时抛出
     */
    BorrowRecord getBorrowById(Long id);
}
