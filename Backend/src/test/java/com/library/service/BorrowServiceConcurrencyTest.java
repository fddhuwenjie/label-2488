package com.library.service;

import com.library.entity.Book;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@SpringBootTest
@DisplayName("BorrowService 并发集成测试")
@Sql(scripts = "classpath:sql/concurrency-test-data.sql", config = @SqlConfig(encoding = "UTF-8"))
@Sql(scripts = "classpath:sql/concurrency-test-cleanup.sql", executionPhase = AFTER_TEST_METHOD, config = @SqlConfig(encoding = "UTF-8"))
class BorrowServiceConcurrencyTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowMapper borrowMapper;

    private static final Long BOOK_ID = 1000L;
    private static final int THREAD_COUNT = 10;
    private static final int INITIAL_STOCK = 3;

    @Test
    @DisplayName("10 个线程同时借阅库存为 3 的图书，最终只有 3 个成功，其余 7 个抛异常，available_stock 最终为 0")
    void should_only_3_success_when_10_threads_borrow_book_with_stock_3() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 1; i <= THREAD_COUNT; i++) {
            final long userId = 1000 + i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    borrowService.borrowBook(userId, BOOK_ID);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - INITIAL_STOCK);

        Book book = bookMapper.findById(BOOK_ID);
        assertThat(book).isNotNull();
        assertThat(book.getAvailableStock()).isEqualTo(0);

        int borrowedCount = borrowMapper.countActiveByUserId(1L);
        assertThat(borrowedCount).isGreaterThanOrEqualTo(0);
    }
}
