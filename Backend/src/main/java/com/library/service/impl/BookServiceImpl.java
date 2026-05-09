package com.library.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.library.common.PageResult;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import com.library.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图书业务逻辑实现类
 *
 * <p>提供图书 CRUD 操作及馆藏库存管理，查询操作走 MyBatis 二级缓存，
 * 写操作（新增/更新/删除）通过事务保证一致性并自动清除对应缓存。
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    @Autowired
    private BookMapper bookMapper;

    /**
     * 分页查询图书列表实现。
     * 使用 PageHelper 物理分页，支持按关键字（书名/作者/ISBN）和分类过滤
     */
    @Override
    public PageResult<Book> getAllBooks(String keyword, String category, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Book> books = bookMapper.findAll(keyword, category);
        PageInfo<Book> pageInfo = new PageInfo<>(books);
        return PageResult.of(pageInfo);
    }

    @Override
    public Book getBookById(Long id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new RuntimeException("图书不存在或已下架：ID=" + id);
        }
        return book;
    }

    /**
     * 新增图书实现。
     * ISBN 唯一性校验后插入，初始可借数量等于总馆藏数量
     */
    @Override
    @Transactional
    public Book createBook(BookRequest request) {
        // ISBN 去重校验（ISBN 可为空，只对非空 ISBN 校验）
        if (request.getIsbn() != null && !request.getIsbn().isEmpty()
                && bookMapper.findByIsbn(request.getIsbn()) != null) {
            throw new RuntimeException("ISBN 已存在：" + request.getIsbn());
        }

        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setTotalStock(request.getTotalStock());
        // 新书入库时，可借数量初始等于馆藏总数
        book.setAvailableStock(request.getTotalStock());
        book.setDescription(request.getDescription());

        bookMapper.insert(book);
        log.info("图书新增成功: 《{}》, ID={}", book.getTitle(), book.getId());

        // 重新查询确保返回完整数据（含自动填充的时间戳）
        return bookMapper.findById(book.getId());
    }

    /**
     * 更新图书信息实现。
     * 使用动态 SQL 局部更新，不影响 available_stock（库存由借还操作单独管理）
     */
    @Override
    @Transactional
    public Book updateBook(Long id, BookRequest request) {
        if (bookMapper.findById(id) == null) {
            throw new RuntimeException("图书不存在或已下架：ID=" + id);
        }

        Book book = new Book();
        book.setId(id);
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setTotalStock(request.getTotalStock());
        book.setDescription(request.getDescription());

        bookMapper.update(book);
        log.info("图书信息已更新: ID={}", id);
        return bookMapper.findById(id);
    }

    /**
     * 逻辑删除（下架）图书实现。
     * 将 status 设为 0，保留记录以维护历史借阅数据的完整性
     */
    @Override
    @Transactional
    public void deleteBook(Long id) {
        if (bookMapper.findById(id) == null) {
            throw new RuntimeException("图书不存在或已下架：ID=" + id);
        }
        bookMapper.deleteById(id);
        log.info("图书已下架（逻辑删除）: ID={}", id);
    }
}
