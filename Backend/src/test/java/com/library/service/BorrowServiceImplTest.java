package com.library.service;

import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowMapper;
import com.library.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * BorrowServiceImpl 单元测试
 *
 * <p>使用 Mockito 完整隔离 BorrowMapper / BookMapper，专注验证
 * 借阅业务的核心分支：超借限制、库存扣减、并发安全、归还校验。
 * 不启动 Spring 上下文，执行速度极快。
 */
@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private static final Long USER_ID   = 1L;
    private static final Long BOOK_ID   = 10L;
    private static final Long RECORD_ID = 100L;

    private Book availableBook;
    private BorrowRecord activeBorrowRecord;

    @BeforeEach
    void setUp() {
        availableBook = new Book();
        availableBook.setId(BOOK_ID);
        availableBook.setTitle("Spring Boot 实战");
        availableBook.setAvailableStock(3);
        availableBook.setStatus(1);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setPassword("$2a$10$hashedSecret");  // 必须在 getBorrowById 后被清空

        activeBorrowRecord = new BorrowRecord();
        activeBorrowRecord.setId(RECORD_ID);
        activeBorrowRecord.setUserId(USER_ID);
        activeBorrowRecord.setBookId(BOOK_ID);
        activeBorrowRecord.setBorrowDate(LocalDate.now().minusDays(5));
        activeBorrowRecord.setDueDate(LocalDate.now().plusDays(25));
        activeBorrowRecord.setStatus("BORROWED");
        activeBorrowRecord.setUser(user);
        activeBorrowRecord.setBook(availableBook);
    }

    // =========================================================
    // borrowBook
    // =========================================================

    @Nested
    @DisplayName("borrowBook — 超借限制")
    class BorrowLimit {

        @Test
        @DisplayName("当前借阅数已达上限（5本）时拒绝，不查询图书")
        void rejectWhenAtLimit() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(5);

            assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("借阅数量已达上限");

            // 校验：不应访问图书库
            then(bookMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("当前借阅数为 4 本时仍可借阅（边界值：4 < 5）")
        void allowWhenBelowLimit() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(4);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);
            given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(1);
            stubInsertWithAutoId();
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            assertThatCode(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("borrowBook — 图书校验")
    class BookValidation {

        @Test
        @DisplayName("图书不存在时抛出异常")
        void bookNotFound() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
            given(bookMapper.findById(BOOK_ID)).willReturn(null);

            assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("图书不存在或已下架");
        }

        @Test
        @DisplayName("图书可借库存为 0 时拒绝，不执行 SQL 扣减")
        void noAvailableStock() {
            availableBook.setAvailableStock(0);
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);

            assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("暂无可借库存");

            then(bookMapper).should(never()).decreaseAvailableStock(anyLong());
        }

        @Test
        @DisplayName("并发场景：读时有库存但扣减时返回 0（WHERE available_stock > 0 未命中）")
        void concurrentStockExhausted() {
            // available_stock=3 通过内存校验，但 SQL 层 WHERE available_stock>0 并发冲突
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);
            given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(0);

            assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("图书库存不足");

            // 并发失败后不应插入借阅记录
            then(borrowMapper).should(never()).insert(any());
        }
    }

    @Nested
    @DisplayName("borrowBook — 成功路径")
    class BorrowSuccess {

        @Test
        @DisplayName("正常借阅：库存扣减 1、创建 BORROWED 记录、应还日期 = 今天 + 30 天")
        void success() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(2);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);
            given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(1);
            stubInsertWithAutoId();
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.borrowBook(USER_ID, BOOK_ID);

            // 验证库存扣减被调用
            then(bookMapper).should().decreaseAvailableStock(BOOK_ID);

            // 验证插入的记录字段正确
            then(borrowMapper).should().insert(argThat(r ->
                    "BORROWED".equals(r.getStatus())
                    && LocalDate.now().plusDays(30).equals(r.getDueDate())
                    && USER_ID.equals(r.getUserId())
                    && BOOK_ID.equals(r.getBookId())
            ));

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("BORROWED");
        }

        @Test
        @DisplayName("借阅成功后返回的记录中用户密码字段被清空（信息脱敏）")
        void passwordMaskedAfterBorrow() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);
            given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(1);
            stubInsertWithAutoId();
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.borrowBook(USER_ID, BOOK_ID);

            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getPassword())
                    .as("借阅结果中用户密码必须为 null")
                    .isNull();
        }
    }

    // =========================================================
    // returnBook
    // =========================================================

    @Nested
    @DisplayName("returnBook — 校验分支")
    class ReturnValidation {

        @Test
        @DisplayName("借阅记录不存在时抛出异常")
        void recordNotFound() {
            given(borrowMapper.findById(999L)).willReturn(null);

            assertThatThrownBy(() -> borrowService.returnBook(999L, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("借阅记录不存在");
        }

        @Test
        @DisplayName("归还他人的借阅记录时抛出权限异常，不修改库存")
        void notOwner() {
            activeBorrowRecord.setUserId(99L);  // 归属其他用户
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            assertThatThrownBy(() -> borrowService.returnBook(RECORD_ID, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("无权操作他人");

            then(bookMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("重复归还已还书的记录时抛出异常")
        void alreadyReturned() {
            activeBorrowRecord.setStatus("RETURNED");
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            assertThatThrownBy(() -> borrowService.returnBook(RECORD_ID, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("已归还");
        }
    }

    @Nested
    @DisplayName("returnBook — 成功路径")
    class ReturnSuccess {

        @Test
        @DisplayName("正常还书：状态变 RETURNED、returnDate = 今天、库存 +1")
        void success() {
            // 第一次 findById：校验记录归属；第二次：getBorrowById 组装返回值
            BorrowRecord returnedView = buildReturnedRecord();
            given(borrowMapper.findById(RECORD_ID))
                    .willReturn(activeBorrowRecord)
                    .willReturn(returnedView);

            BorrowRecord result = borrowService.returnBook(RECORD_ID, USER_ID);

            // 验证 update 调用时字段已更新
            then(borrowMapper).should().update(argThat(r ->
                    "RETURNED".equals(r.getStatus())
                    && LocalDate.now().equals(r.getReturnDate())
            ));

            // 验证库存已恢复
            then(bookMapper).should().increaseAvailableStock(BOOK_ID);

            assertThat(result.getStatus()).isEqualTo("RETURNED");
            assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("还书后库存恢复调用的是 increaseAvailableStock，且传入正确的 bookId")
        void stockRestoredForCorrectBook() {
            BorrowRecord returnedView = buildReturnedRecord();
            given(borrowMapper.findById(RECORD_ID))
                    .willReturn(activeBorrowRecord)
                    .willReturn(returnedView);

            borrowService.returnBook(RECORD_ID, USER_ID);

            then(bookMapper).should().increaseAvailableStock(BOOK_ID);
            then(bookMapper).should(never()).increaseAvailableStock(argThat(id -> !id.equals(BOOK_ID)));
        }
    }

    // =========================================================
    // getBorrowById
    // =========================================================

    @Nested
    @DisplayName("getBorrowById")
    class GetBorrowById {

        @Test
        @DisplayName("记录不存在时抛出 RuntimeException")
        void notFound() {
            given(borrowMapper.findById(999L)).willReturn(null);

            assertThatThrownBy(() -> borrowService.getBorrowById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("借阅记录不存在");
        }

        @Test
        @DisplayName("查询成功，返回的记录中用户密码被置为 null")
        void passwordMasked() {
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.getBorrowById(RECORD_ID);

            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getPassword())
                    .as("getBorrowById 必须清空密码字段防止泄露")
                    .isNull();
        }

        @Test
        @DisplayName("关联 User 为 null 时不抛出 NullPointerException")
        void userIsNull_noException() {
            activeBorrowRecord.setUser(null);
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            assertThatCode(() -> borrowService.getBorrowById(RECORD_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("关联 Book 为 null 时不抛出 NullPointerException")
        void bookIsNull_noException() {
            activeBorrowRecord.setBook(null);
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            assertThatCode(() -> borrowService.getBorrowById(RECORD_ID))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================
    // 辅助方法
    // =========================================================

    /**
     * 模拟 MyBatis insert 后主键回填行为：给 BorrowRecord 设置生成的 ID。
     */
    private void stubInsertWithAutoId() {
        doAnswer(invocation -> {
            BorrowRecord r = invocation.getArgument(0);
            r.setId(RECORD_ID);
            return 1;
        }).when(borrowMapper).insert(any(BorrowRecord.class));
    }

    private BorrowRecord buildReturnedRecord() {
        BorrowRecord r = new BorrowRecord();
        r.setId(RECORD_ID);
        r.setUserId(USER_ID);
        r.setBookId(BOOK_ID);
        r.setStatus("RETURNED");
        r.setReturnDate(LocalDate.now());
        r.setBorrowDate(activeBorrowRecord.getBorrowDate());
        r.setDueDate(activeBorrowRecord.getDueDate());
        return r;
    }
}
