package com.library.service;

import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowMapper;
import com.library.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowService 单元测试")
class BorrowServiceUnitTest {

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private static final Long USER_ID = 1L;
    private static final Long BOOK_ID = 10L;
    private static final Long RECORD_ID = 100L;

    @Test
    @DisplayName("正常借阅成功时验证 decreaseAvailableStock 被调用且返回 1")
    void should_call_decreaseAvailableStock_and_return_success_when_borrow_book_normally() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Java 实战");
        book.setAvailableStock(3);
        book.setStatus(1);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole("ROLE_USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        BorrowRecord savedRecord = new BorrowRecord();
        savedRecord.setId(RECORD_ID);
        savedRecord.setUserId(USER_ID);
        savedRecord.setBookId(BOOK_ID);
        savedRecord.setBorrowDate(LocalDate.now());
        savedRecord.setDueDate(LocalDate.now().plusDays(30));
        savedRecord.setStatus("BORROWED");
        savedRecord.setUser(user);
        savedRecord.setBook(book);

        given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
        given(bookMapper.findById(BOOK_ID)).willReturn(book);
        given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(1);
        given(borrowMapper.insert(any(BorrowRecord.class))).willAnswer(invocation -> {
            BorrowRecord r = invocation.getArgument(0);
            r.setId(RECORD_ID);
            return 1;
        });
        given(borrowMapper.findById(RECORD_ID)).willReturn(savedRecord);

        BorrowRecord result = borrowService.borrowBook(USER_ID, BOOK_ID);

        verify(bookMapper).decreaseAvailableStock(BOOK_ID);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(RECORD_ID);
        assertThat(result.getStatus()).isEqualTo("BORROWED");
    }

    @Test
    @DisplayName("乐观锁失败（返回 0）时验证抛出异常且 borrowMapper.insert 未被调用")
    void should_throw_exception_and_not_call_insert_when_optimistic_lock_fails() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Java 实战");
        book.setAvailableStock(3);
        book.setStatus(1);

        given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
        given(bookMapper.findById(BOOK_ID)).willReturn(book);
        given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(0);

        assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("图书库存不足");

        verify(bookMapper).decreaseAvailableStock(BOOK_ID);
        verify(borrowMapper, never()).insert(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("借阅上限 5 本时 countActiveByUserId 返回 5 应抛出异常")
    void should_throw_exception_when_user_has_reached_borrow_limit_of_5() {
        given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(5);

        assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("借阅数量已达上限");

        verify(bookMapper, never()).findById(anyLong());
        verify(bookMapper, never()).decreaseAvailableStock(anyLong());
        verify(borrowMapper, never()).insert(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("归还时验证 increaseAvailableStock 仅在状态更新成功后调用")
    void should_call_increaseAvailableStock_only_after_status_update_succeeds_when_returning_book() {
        BorrowRecord activeRecord = new BorrowRecord();
        activeRecord.setId(RECORD_ID);
        activeRecord.setUserId(USER_ID);
        activeRecord.setBookId(BOOK_ID);
        activeRecord.setBorrowDate(LocalDate.now().minusDays(10));
        activeRecord.setDueDate(LocalDate.now().plusDays(20));
        activeRecord.setStatus("BORROWED");

        BorrowRecord returnedRecord = new BorrowRecord();
        returnedRecord.setId(RECORD_ID);
        returnedRecord.setUserId(USER_ID);
        returnedRecord.setBookId(BOOK_ID);
        returnedRecord.setBorrowDate(LocalDate.now().minusDays(10));
        returnedRecord.setDueDate(LocalDate.now().plusDays(20));
        returnedRecord.setReturnDate(LocalDate.now());
        returnedRecord.setStatus("RETURNED");

        given(borrowMapper.findById(RECORD_ID))
                .willReturn(activeRecord)
                .willReturn(returnedRecord);
        given(borrowMapper.update(any(BorrowRecord.class))).willReturn(1);
        given(bookMapper.increaseAvailableStock(BOOK_ID)).willReturn(1);

        BorrowRecord result = borrowService.returnBook(RECORD_ID, USER_ID);

        verify(borrowMapper).update(any(BorrowRecord.class));
        verify(bookMapper).increaseAvailableStock(BOOK_ID);
        assertThat(result.getStatus()).isEqualTo("RETURNED");
        assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("getBorrowById 中延迟加载代理对象拷贝逻辑验证返回对象不是原始代理且字段正确拷贝")
    void should_return_clean_pojo_not_proxy_with_correct_fields_when_getBorrowById() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setRole("ROLE_USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Book book = new Book();
        book.setId(BOOK_ID);
        book.setIsbn("978-1234567890");
        book.setTitle("Java 实战");
        book.setAuthor("Brian Goetz");
        book.setPublisher("机械工业出版社");
        book.setCategory("计算机");
        book.setTotalStock(10);
        book.setAvailableStock(5);
        book.setDescription("Java 经典书籍");
        book.setStatus(1);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());

        BorrowRecord proxyRecord = new BorrowRecord();
        proxyRecord.setId(RECORD_ID);
        proxyRecord.setUserId(USER_ID);
        proxyRecord.setBookId(BOOK_ID);
        proxyRecord.setBorrowDate(LocalDate.now().minusDays(5));
        proxyRecord.setDueDate(LocalDate.now().plusDays(25));
        proxyRecord.setReturnDate(null);
        proxyRecord.setStatus("BORROWED");
        proxyRecord.setCreatedAt(LocalDateTime.now());
        proxyRecord.setUser(user);
        proxyRecord.setBook(book);

        given(borrowMapper.findById(RECORD_ID)).willReturn(proxyRecord);

        BorrowRecord result = borrowService.getBorrowById(RECORD_ID);

        assertThat(result).isNotNull();
        assertThat(result).isNotSameAs(proxyRecord);

        assertThat(result.getId()).isEqualTo(RECORD_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getBookId()).isEqualTo(BOOK_ID);
        assertThat(result.getBorrowDate()).isEqualTo(LocalDate.now().minusDays(5));
        assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(25));
        assertThat(result.getReturnDate()).isNull();
        assertThat(result.getStatus()).isEqualTo("BORROWED");
        assertThat(result.getCreatedAt()).isEqualTo(proxyRecord.getCreatedAt());

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser()).isNotSameAs(user);
        assertThat(result.getUser().getId()).isEqualTo(USER_ID);
        assertThat(result.getUser().getUsername()).isEqualTo("testuser");
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUser().getPhone()).isEqualTo("13800138000");
        assertThat(result.getUser().getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getUser().getStatus()).isEqualTo(1);
        assertThat(result.getUser().getCreatedAt()).isEqualTo(user.getCreatedAt());
        assertThat(result.getUser().getUpdatedAt()).isEqualTo(user.getUpdatedAt());

        assertThat(result.getBook()).isNotNull();
        assertThat(result.getBook()).isNotSameAs(book);
        assertThat(result.getBook().getId()).isEqualTo(BOOK_ID);
        assertThat(result.getBook().getIsbn()).isEqualTo("978-1234567890");
        assertThat(result.getBook().getTitle()).isEqualTo("Java 实战");
        assertThat(result.getBook().getAuthor()).isEqualTo("Brian Goetz");
        assertThat(result.getBook().getPublisher()).isEqualTo("机械工业出版社");
        assertThat(result.getBook().getCategory()).isEqualTo("计算机");
        assertThat(result.getBook().getTotalStock()).isEqualTo(10);
        assertThat(result.getBook().getAvailableStock()).isEqualTo(5);
        assertThat(result.getBook().getDescription()).isEqualTo("Java 经典书籍");
        assertThat(result.getBook().getStatus()).isEqualTo(1);
        assertThat(result.getBook().getCreatedAt()).isEqualTo(book.getCreatedAt());
        assertThat(result.getBook().getUpdatedAt()).isEqualTo(book.getUpdatedAt());
    }
}
