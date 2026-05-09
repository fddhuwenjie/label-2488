package com.library.service;

import com.library.common.PageResult;
import com.library.dto.BookRequest;
import com.library.entity.Book;

/**
 * 图书业务服务接口
 * 定义图书模块的核心业务操作，由 {@link impl.BookServiceImpl} 提供具体实现
 */
public interface BookService {

    /**
     * 分页查询图书列表（支持关键字和分类过滤）
     *
     * @param keyword  搜索关键字，匹配书名/作者/ISBN，null 表示不过滤
     * @param category 图书分类，null 表示不过滤
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 分页图书列表
     */
    PageResult<Book> getAllBooks(String keyword, String category, int pageNum, int pageSize);

    /**
     * 根据 ID 获取图书详情
     *
     * @param id 图书 ID
     * @return 图书实体
     * @throws RuntimeException 图书不存在或已下架时抛出
     */
    Book getBookById(Long id);

    /**
     * 新增图书（管理员专用）
     *
     * @param request 图书信息（含参数校验）
     * @return 创建成功的图书实体（含自动生成的 ID）
     * @throws RuntimeException ISBN 重复时抛出
     */
    Book createBook(BookRequest request);

    /**
     * 更新图书信息（管理员专用，局部更新）
     *
     * @param id      目标图书 ID
     * @param request 更新内容
     * @return 更新后的图书实体
     * @throws RuntimeException 图书不存在时抛出
     */
    Book updateBook(Long id, BookRequest request);

    /**
     * 逻辑删除图书（下架，管理员专用）
     * 采用逻辑删除而非物理删除，以保留历史借阅记录的完整性
     *
     * @param id 目标图书 ID
     * @throws RuntimeException 图书不存在时抛出
     */
    void deleteBook(Long id);
}
