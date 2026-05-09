package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.service.BookService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 图书管理控制器
 *
 * <p>提供图书 CRUD 的 RESTful 接口，遵循 RESTful 规范：
 * <ul>
 *   <li>GET：查询（幂等、安全）</li>
 *   <li>POST：创建（非幂等）</li>
 *   <li>PUT：全量/部分更新（幂等）</li>
 *   <li>DELETE：删除（幂等）</li>
 * </ul>
 *
 * <p>权限说明：查询接口对所有已登录用户开放，增删改仅限 ADMIN 角色。
 * 权限在 SecurityConfig 路由层和 @PreAuthorize 方法层双重控制。
 */
@Slf4j
@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * 分页查询图书列表（GET /api/books）
     * 支持关键字搜索（书名/作者/ISBN）和分类过滤，任意已登录用户可访问
     *
     * @param keyword  搜索关键字（可选）
     * @param category 图书分类（可选）
     * @param pageNum  页码，默认 1
     * @param pageSize 每页数量，默认 10
     * @return 分页图书列表
     */
    @GetMapping
    public Result<PageResult<Book>> getAllBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Book> result = bookService.getAllBooks(keyword, category, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取图书详情（GET /api/books/{id}）
     *
     * @param id 图书 ID
     * @return 图书完整信息
     */
    @GetMapping("/{id}")
    public Result<Book> getBookById(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        return Result.success(book);
    }

    /**
     * 新增图书（POST /api/books）
     * 仅 ADMIN 角色可操作
     *
     * <p>请求体示例：
     * <pre>
     * {
     *   "isbn": "978-7-115-54742-4",
     *   "title": "Spring Boot 实战",
     *   "author": "克雷格·沃斯",
     *   "publisher": "人民邮电出版社",
     *   "category": "计算机",
     *   "totalStock": 5,
     *   "description": "Spring Boot 权威指南"
     * }
     * </pre>
     *
     * @param request 图书信息（@Valid 触发参数合法性校验）
     * @return 新增的图书（含自动生成的 ID 和时间戳）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Book> createBook(@Valid @RequestBody BookRequest request) {
        Book book = bookService.createBook(request);
        return Result.success("图书添加成功", book);
    }

    /**
     * 更新图书信息（PUT /api/books/{id}）
     * 仅 ADMIN 角色可操作
     *
     * @param id      目标图书 ID
     * @param request 更新内容
     * @return 更新后的图书信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Book> updateBook(@PathVariable Long id,
                                   @Valid @RequestBody BookRequest request) {
        Book book = bookService.updateBook(id, request);
        return Result.success("图书信息更新成功", book);
    }

    /**
     * 下架图书（DELETE /api/books/{id}）
     * 采用逻辑删除，将 status 设为 0，仅 ADMIN 角色可操作
     *
     * @param id 目标图书 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success("图书已下架", null);
    }
}
