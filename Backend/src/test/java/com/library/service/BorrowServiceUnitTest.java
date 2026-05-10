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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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

    private Book availableBook;
    private BorrowRecord activeBorrowRecord;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setEmail("test@library.com");
        testUser.setPhone("13800000000");
        testUser.setRole("ROLE_USER");
        testUser.setStatus(1);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        availableBook = new Book();
        availableBook.setId(BOOK_ID);
        availableBook.setTitle("Spring Boot 实战");
        availableBook.setAvailableStock(3);
        availableBook.setStatus(1);

        activeBorrowRecord = new BorrowRecord();
        activeBorrowRecord.setId(RECORD_ID);
        activeBorrowRecord.setUserId(USER_ID);
        activeBorrowRecord.setBookId(BOOK_ID);
        activeBorrowRecord.setBorrowDate(LocalDate.now());
        activeBorrowRecord.setDueDate(LocalDate.now().plusDays(30));
        activeBorrowRecord.setStatus("BORROWED");
        activeBorrowRecord.setCreatedAt(LocalDateTime.now());
        activeBorrowRecord.setUser(testUser);
        activeBorrowRecord.setBook(availableBook);
    }

    @Nested
    @DisplayName("正常借阅成功")
    class BorrowSuccess {

        @Test
        @DisplayName("should_验证decreaseAvailableStock被调用且返回1_when_正常借阅成功")
        void should_verifyDecreaseStockCalledAndReturnRecord_when_borrowSuccess() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(2);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);
            given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(1);
            stubInsertWithAutoId();
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.borrowBook(USER_ID, BOOK_ID);

            then(bookMapper).should().decreaseAvailableStock(BOOK_ID);
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RECORD_ID);
            assertThat(result.getStatus()).isEqualTo("BORROWED");
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getBookId()).isEqualTo(BOOK_ID);
            assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));

            then(borrowMapper).should().insert(argThat(r ->
                    "BORROWED".equals(r.getStatus())
                            && USER_ID.equals(r.getUserId())
                            && BOOK_ID.equals(r.getBookId())
                            && LocalDate.now().plusDays(30).equals(r.getDueDate())
            ));
        }
    }

    @Nested
    @DisplayName("乐观锁失败")
    class OptimisticLockFailure {

        @Test
        @DisplayName("should_抛出异常且insert未被调用_when_乐观锁库存扣减返回0")
        void should_throwExceptionAndInsertNotCalled_when_optimisticLockReturnsZero() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(0);
            given(bookMapper.findById(BOOK_ID)).willReturn(availableBook);
            given(bookMapper.decreaseAvailableStock(BOOK_ID)).willReturn(0);

            assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("图书库存不足");

            then(borrowMapper).should(never()).insert(any(BorrowRecord.class));
        }
    }

    @Nested
    @DisplayName("借阅上限检查")
    class BorrowLimitCheck {

        @Test
        @DisplayName("should_抛出借阅上限异常_when_countActiveByUserId返回5")
        void should_throwBorrowLimitException_when_countActiveReturnsFive() {
            given(borrowMapper.countActiveByUserId(USER_ID)).willReturn(5);

            assertThatThrownBy(() -> borrowService.borrowBook(USER_ID, BOOK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("借阅数量已达上限");

            then(bookMapper).shouldHaveNoInteractions();
            then(borrowMapper).should(never()).insert(any(BorrowRecord.class));
        }
    }

    @Nested
    @DisplayName("归还图书")
    class ReturnBook {

        @Test
        @DisplayName("should_仅在状态更新成功后调用increaseAvailableStock_when_归还图书成功")
        void should_callIncreaseStockOnlyAfterStatusUpdate_when_returnBookSuccess() {
            BorrowRecord returnedRecord = new BorrowRecord();
            returnedRecord.setId(RECORD_ID);
            returnedRecord.setUserId(USER_ID);
            returnedRecord.setBookId(BOOK_ID);
            returnedRecord.setBorrowDate(LocalDate.now().minusDays(5));
            returnedRecord.setDueDate(LocalDate.now().plusDays(25));
            returnedRecord.setReturnDate(LocalDate.now());
            returnedRecord.setStatus("RETURNED");
            returnedRecord.setCreatedAt(LocalDateTime.now());
            returnedRecord.setUser(testUser);
            returnedRecord.setBook(availableBook);

            given(borrowMapper.findById(RECORD_ID))
                    .willReturn(activeBorrowRecord)
                    .willReturn(returnedRecord);

            BorrowRecord result = borrowService.returnBook(RECORD_ID, USER_ID);

            then(borrowMapper).should().update(argThat(r ->
                    "RETURNED".equals(r.getStatus())
                            && LocalDate.now().equals(r.getReturnDate())
            ));

            then(bookMapper).should().increaseAvailableStock(BOOK_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("RETURNED");
            assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("should_不调用increaseAvailableStock_when_借阅记录不存在")
        void should_notCallIncreaseStock_when_recordNotFound() {
            given(borrowMapper.findById(999L)).willReturn(null);

            assertThatThrownBy(() -> borrowService.returnBook(999L, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("借阅记录不存在");

            then(bookMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("should_不调用increaseAvailableStock_when_非本人借阅记录")
        void should_notCallIncreaseStock_when_notOwner() {
            activeBorrowRecord.setUserId(99L);
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            assertThatThrownBy(() -> borrowService.returnBook(RECORD_ID, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("无权操作他人");

            then(bookMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("延迟加载代理对象拷贝")
    class LazyProxyCopy {

        @Test
        @DisplayName("should_返回对象不是原始代理且字段正确拷贝_when_getBorrowById延迟加载代理拷贝")
        void should_returnNotSameObjectAndFieldsCopied_when_getBorrowByIdLazyProxyCopy() {
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.getBorrowById(RECORD_ID);

            assertThat(result).isNotSameAs(activeBorrowRecord);

            assertThat(result.getId()).isEqualTo(activeBorrowRecord.getId());
            assertThat(result.getUserId()).isEqualTo(activeBorrowRecord.getUserId());
            assertThat(result.getBookId()).isEqualTo(activeBorrowRecord.getBookId());
            assertThat(result.getBorrowDate()).isEqualTo(activeBorrowRecord.getBorrowDate());
            assertThat(result.getDueDate()).isEqualTo(activeBorrowRecord.getDueDate());
            assertThat(result.getReturnDate()).isEqualTo(activeBorrowRecord.getReturnDate());
            assertThat(result.getStatus()).isEqualTo(activeBorrowRecord.getStatus());
            assertThat(result.getCreatedAt()).isEqualTo(activeBorrowRecord.getCreatedAt());

            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser()).isNotSameAs(activeBorrowRecord.getUser());
            assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(result.getUser().getUsername()).isEqualTo(testUser.getUsername());
            assertThat(result.getUser().getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.getUser().getPhone()).isEqualTo(testUser.getPhone());
            assertThat(result.getUser().getRole()).isEqualTo(testUser.getRole());
            assertThat(result.getUser().getStatus()).isEqualTo(testUser.getStatus());

            assertThat(result.getBook()).isNotNull();
            assertThat(result.getBook()).isNotSameAs(activeBorrowRecord.getBook());
            assertThat(result.getBook().getId()).isEqualTo(availableBook.getId());
            assertThat(result.getBook().getTitle()).isEqualTo(availableBook.getTitle());
        }

        @Test
        @DisplayName("should_不抛出NullPointerException_when_关联User为null")
        void should_notThrowNPE_when_userIsNull() {
            activeBorrowRecord.setUser(null);
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.getBorrowById(RECORD_ID);

            assertThat(result).isNotNull();
            assertThat(result.getUser()).isNull();
        }

        @Test
        @DisplayName("should_不抛出NullPointerException_when_关联Book为null")
        void should_notThrowNPE_when_bookIsNull() {
            activeBorrowRecord.setBook(null);
            given(borrowMapper.findById(RECORD_ID)).willReturn(activeBorrowRecord);

            BorrowRecord result = borrowService.getBorrowById(RECORD_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBook()).isNull();
        }
    }

    private void stubInsertWithAutoId() {
        given(borrowMapper.insert(any(BorrowRecord.class))).willAnswer(invocation -> {
            BorrowRecord r = invocation.getArgument(0);
            r.setId(RECORD_ID);
            return 1;
        });
    }
}
