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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

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
    void should_call_decreaseAvailableStock_and_return_1_when_borrow_success() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Spring Boot 实战");
        book.setAvailableStock(3);
        book.setStatus(1);

        given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
        given(bookMapper.findById(BOOK_ID)).willReturn(book);
        given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(1);
        willAnswer(invocation -> {
            BorrowRecord r = invocation.getArgument(0);
            r.setId(RECORD_ID);
            return 1;
        }).given(borrowMapper).insert(any(BorrowRecord.class));

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        BorrowRecord record = new BorrowRecord();
        record.setId(RECORD_ID);
        record.setUserId(USER_ID);
        record.setBookId(BOOK_ID);
        record.setStatus("BORROWED");
        record.setUser(user);
        record.setBook(book);
        given(borrowMapper.findById(RECORD_ID)).willReturn(record);

        BorrowRecord result = borrowService.borrowBook(USER_ID, BOOK_ID);

        then(bookMapper).should().decreaseAvailableStock(BOOK_ID);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("BORROWED");
    }

    @Test
    @DisplayName("乐观锁失败（返回 0）时验证抛出异常且 borrowMapper.insert 未被调用")
    void should_throw_exception_and_not_call_insert_when_optimistic_lock_fails() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Spring Boot 实战");
        book.setAvailableStock(3);
        book.setStatus(1);

        given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
        given(bookMapper.findById(BOOK_ID)).willReturn(book);
        given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(0);

        assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("图书库存不足");

        then(borrowMapper).should(never()).insert(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("借阅上限 5 本时 countActiveByUserId 返回 5 应抛出异常")
    void should_throw_exception_when_countActiveByUserId_returns_5() {
        given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(5);

        assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("借阅数量已达上限");

        then(bookMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("归还时验证 increaseAvailableStock 仅在状态更新成功后调用")
    void should_call_increaseAvailableStock_only_after_status_update_success_when_return_book() {
        BorrowRecord activeRecord = new BorrowRecord();
        activeRecord.setId(RECORD_ID);
        activeRecord.setUserId(USER_ID);
        activeRecord.setBookId(BOOK_ID);
        activeRecord.setStatus("BORROWED");

        BorrowRecord returnedRecord = new BorrowRecord();
        returnedRecord.setId(RECORD_ID);
        returnedRecord.setUserId(USER_ID);
        returnedRecord.setBookId(BOOK_ID);
        returnedRecord.setStatus("RETURNED");
        returnedRecord.setReturnDate(LocalDate.now());

        given(borrowMapper.findById(RECORD_ID))
                .willReturn(activeRecord)
                .willReturn(returnedRecord);

        borrowService.returnBook(RECORD_ID, USER_ID);

        then(borrowMapper).should().update(any(BorrowRecord.class));
        then(bookMapper).should().increaseAvailableStock(BOOK_ID);
    }

    @Test
    @DisplayName("getBorrowById 中延迟加载代理对象拷贝逻辑验证返回对象不是原始代理且字段正确拷贝")
    void should_return_new_object_not_proxy_and_copy_fields_correctly_when_getBorrowById() {
        User proxyUser = new User();
        proxyUser.setId(USER_ID);
        proxyUser.setUsername("testuser");
        proxyUser.setEmail("test@example.com");
        proxyUser.setPhone("13800138000");
        proxyUser.setRole("ROLE_USER");
        proxyUser.setStatus(1);
        proxyUser.setCreatedAt(LocalDateTime.now());
        proxyUser.setUpdatedAt(LocalDateTime.now());

        Book proxyBook = new Book();
        proxyBook.setId(BOOK_ID);
        proxyBook.setIsbn("978-1234567890");
        proxyBook.setTitle("Spring Boot 实战");
        proxyBook.setAuthor("Craig Walls");
        proxyBook.setPublisher("Publisher");
        proxyBook.setCategory("计算机");
        proxyBook.setTotalStock(10);
        proxyBook.setAvailableStock(5);
        proxyBook.setDescription("图书描述");
        proxyBook.setStatus(1);
        proxyBook.setCreatedAt(LocalDateTime.now());
        proxyBook.setUpdatedAt(LocalDateTime.now());

        BorrowRecord proxyRecord = new BorrowRecord();
        proxyRecord.setId(RECORD_ID);
        proxyRecord.setUserId(USER_ID);
        proxyRecord.setBookId(BOOK_ID);
        proxyRecord.setBorrowDate(LocalDate.now().minusDays(5));
        proxyRecord.setDueDate(LocalDate.now().plusDays(25));
        proxyRecord.setReturnDate(null);
        proxyRecord.setStatus("BORROWED");
        proxyRecord.setCreatedAt(LocalDateTime.now());
        proxyRecord.setUser(proxyUser);
        proxyRecord.setBook(proxyBook);

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
        assertThat(result.getUser()).isNotSameAs(proxyUser);
        assertThat(result.getUser().getId()).isEqualTo(USER_ID);
        assertThat(result.getUser().getUsername()).isEqualTo("testuser");
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUser().getPhone()).isEqualTo("13800138000");
        assertThat(result.getUser().getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getUser().getStatus()).isEqualTo(1);
        assertThat(result.getUser().getCreatedAt()).isEqualTo(proxyUser.getCreatedAt());
        assertThat(result.getUser().getUpdatedAt()).isEqualTo(proxyUser.getUpdatedAt());

        assertThat(result.getBook()).isNotNull();
        assertThat(result.getBook()).isNotSameAs(proxyBook);
        assertThat(result.getBook().getId()).isEqualTo(BOOK_ID);
        assertThat(result.getBook().getIsbn()).isEqualTo("978-1234567890");
        assertThat(result.getBook().getTitle()).isEqualTo("Spring Boot 实战");
        assertThat(result.getBook().getAuthor()).isEqualTo("Craig Walls");
        assertThat(result.getBook().getPublisher()).isEqualTo("Publisher");
        assertThat(result.getBook().getCategory()).isEqualTo("计算机");
        assertThat(result.getBook().getTotalStock()).isEqualTo(10);
        assertThat(result.getBook().getAvailableStock()).isEqualTo(5);
        assertThat(result.getBook().getDescription()).isEqualTo("图书描述");
        assertThat(result.getBook().getStatus()).isEqualTo(1);
        assertThat(result.getBook().getCreatedAt()).isEqualTo(proxyBook.getCreatedAt());
        assertThat(result.getBook().getUpdatedAt()).isEqualTo(proxyBook.getUpdatedAt());
    }
}
