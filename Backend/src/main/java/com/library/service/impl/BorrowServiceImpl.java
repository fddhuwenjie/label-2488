package com.library.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.library.common.PageResult;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowMapper;
import com.library.service.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅业务逻辑实现类
 *
 * <p>借书和还书操作涉及多表更新（borrow_records + books.available_stock），
 * 必须使用 {@code @Transactional} 保证原子性，任一步骤失败则全部回滚。
 *
 * <p>延迟加载演示：{@link #getBorrowById} 方法在 {@code @Transactional} 事务范围内
 * 显式访问 {@code record.getUser()} 和 {@code record.getBook()}，
 * 触发 MyBatis 的延迟加载机制执行关联 SQL 查询，然后再返回已完整加载的实体。
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class BorrowServiceImpl implements BorrowService {

    /** 每位用户同时借阅图书的最大数量上限 */
    private static final int MAX_BORROW_LIMIT = 5;

    /** 默认借阅期限（自然日） */
    private static final int DEFAULT_BORROW_DAYS = 30;

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private BookMapper bookMapper;

    /**
     * 借阅图书实现。
     *
     * <p>执行顺序：
     * <ol>
     *   <li>查询用户当前借阅数量，超限则拒绝</li>
     *   <li>查询图书是否存在且有可借库存</li>
     *   <li>执行 available_stock - 1（乐观更新，影响行数为 0 说明并发导致库存耗尽）</li>
     *   <li>创建借阅记录，设置借阅日期和应还日期</li>
     * </ol>
     */
    @Override
    @Transactional
    public BorrowRecord borrowBook(Long userId, Long bookId) {
        // 检查借阅数量上限
        int activeCount = borrowMapper.countActiveByUserId(userId);
        if (activeCount >= MAX_BORROW_LIMIT) {
            throw new RuntimeException(
                    "借阅数量已达上限（最多 " + MAX_BORROW_LIMIT + " 本），请先归还部分图书");
        }

        // 验证图书存在且有库存
        Book book = bookMapper.findById(bookId);
        if (book == null) {
            throw new RuntimeException("图书不存在或已下架：ID=" + bookId);
        }
        if (book.getAvailableStock() <= 0) {
            throw new RuntimeException("图书《" + book.getTitle() + "》暂无可借库存，请等待其他读者归还");
        }

        // 乐观更新库存：WHERE available_stock > 0 防止并发超借
        int updated = bookMapper.decreaseAvailableStock(bookId);
        if (updated == 0) {
            throw new RuntimeException("图书库存不足，借阅失败，请稍后重试");
        }

        // 创建借阅记录（借阅期 30 天）
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(DEFAULT_BORROW_DAYS));
        record.setStatus("BORROWED");

        borrowMapper.insert(record);
        log.info("借阅成功：用户ID={}, 图书ID={}, 应还日期={}", userId, bookId, record.getDueDate());

        // 返回详情（触发延迟加载以获取关联对象）
        return getBorrowById(record.getId());
    }

    /**
     * 归还图书实现。
     *
     * <p>验证归属权后，将状态更新为 RETURNED，并恢复图书可借库存。
     * 整个操作在同一事务中完成，确保状态和库存的一致性。
     */
    @Override
    @Transactional
    public BorrowRecord returnBook(Long recordId, Long userId) {
        BorrowRecord record = borrowMapper.findById(recordId);
        if (record == null) {
            throw new RuntimeException("借阅记录不存在：ID=" + recordId);
        }
        // 验证归属权，防止用户还别人的书
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人的借阅记录");
        }
        // 验证状态（已归还的记录不可重复归还）
        if ("RETURNED".equals(record.getStatus())) {
            throw new RuntimeException("该图书已归还，无需重复操作");
        }

        // 更新借阅记录
        record.setReturnDate(LocalDate.now());
        record.setStatus("RETURNED");
        borrowMapper.update(record);

        // 归还图书库存
        bookMapper.increaseAvailableStock(record.getBookId());
        log.info("还书成功：借阅ID={}, 用户ID={}", recordId, userId);

        return getBorrowById(recordId);
    }

    @Override
    public PageResult<BorrowRecord> getBorrowRecords(
            Long userId, String status, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BorrowRecord> records = borrowMapper.findAll(userId, status);
        PageInfo<BorrowRecord> pageInfo = new PageInfo<>(records);
        return PageResult.of(pageInfo);
    }

    /**
     * 查询借阅记录详情并演示 MyBatis 延迟加载。
     *
     * <p>{@code borrowMapper.findById(id)} 返回的 BorrowRecord 中，
     * user 和 book 是 MyBatis 的延迟加载代理对象（尚未执行关联 SQL）。
     * 在 {@code @Transactional} 保持 SqlSession 开启的状态下，
     * 显式调用 {@code record.getUser()} 和 {@code record.getBook()} 会触发
     * 各自的 SELECT 语句（UserMapper.findById / BookMapper.findById），
     * 将关联对象加载到内存后，事务结束、SqlSession 关闭也不影响序列化。
     */
    /**
     * 查询借阅记录详情并演示 MyBatis 延迟加载。
     *
     * <p>{@code borrowMapper.findById(id)} 返回的 BorrowRecord 是 MyBatis 的
     * Javassist 延迟加载代理对象。在 {@code @Transactional} 事务范围内，
     * 显式调用 {@code getUser()} / {@code getBook()} 触发关联 SQL 查询后，
     * 将数据拷贝到干净的 POJO 中返回，避免 Jackson 序列化代理类时报错。
     */
    @Override
    @Transactional
    public BorrowRecord getBorrowById(Long id) {
        BorrowRecord record = borrowMapper.findById(id);
        if (record == null) {
            throw new RuntimeException("借阅记录不存在：ID=" + id);
        }

        // 在事务范围内显式触发延迟加载
        User lazyUser = record.getUser();
        Book lazyBook = record.getBook();

        // 将代理对象的数据拷贝到干净的 POJO，避免 Jackson 序列化 Javassist 代理类失败
        BorrowRecord clean = new BorrowRecord();
        clean.setId(record.getId());
        clean.setUserId(record.getUserId());
        clean.setBookId(record.getBookId());
        clean.setBorrowDate(record.getBorrowDate());
        clean.setDueDate(record.getDueDate());
        clean.setReturnDate(record.getReturnDate());
        clean.setStatus(record.getStatus());
        clean.setCreatedAt(record.getCreatedAt());

        // 拷贝关联对象（延迟加载已触发，此时是真实数据）
        if (lazyUser != null) {
            User cleanUser = new User();
            cleanUser.setId(lazyUser.getId());
            cleanUser.setUsername(lazyUser.getUsername());
            cleanUser.setEmail(lazyUser.getEmail());
            cleanUser.setPhone(lazyUser.getPhone());
            cleanUser.setRole(lazyUser.getRole());
            cleanUser.setStatus(lazyUser.getStatus());
            cleanUser.setCreatedAt(lazyUser.getCreatedAt());
            cleanUser.setUpdatedAt(lazyUser.getUpdatedAt());
            clean.setUser(cleanUser);
        }
        if (lazyBook != null) {
            Book cleanBook = new Book();
            cleanBook.setId(lazyBook.getId());
            cleanBook.setIsbn(lazyBook.getIsbn());
            cleanBook.setTitle(lazyBook.getTitle());
            cleanBook.setAuthor(lazyBook.getAuthor());
            cleanBook.setPublisher(lazyBook.getPublisher());
            cleanBook.setCategory(lazyBook.getCategory());
            cleanBook.setTotalStock(lazyBook.getTotalStock());
            cleanBook.setAvailableStock(lazyBook.getAvailableStock());
            cleanBook.setDescription(lazyBook.getDescription());
            cleanBook.setStatus(lazyBook.getStatus());
            cleanBook.setCreatedAt(lazyBook.getCreatedAt());
            cleanBook.setUpdatedAt(lazyBook.getUpdatedAt());
            clean.setBook(cleanBook);
        }

        return clean;
    }
}
