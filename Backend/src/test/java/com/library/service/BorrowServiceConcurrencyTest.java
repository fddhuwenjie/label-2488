package com.library.service;

import com.library.entity.Book;
import com.library.mapper.BookMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "classpath:com/library/service/borrow-test-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:com/library/service/borrow-cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("BorrowService 并发集成测试")
class BorrowServiceConcurrencyTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookMapper bookMapper;

    private static final Long USER_ID = 100L;
    private static final Long BOOK_ID = 100L;
    private static final int TOTAL_THREADS = 10;
    private static final int INITIAL_STOCK = 3;

    @Test
    @DisplayName("10 个线程同时借阅库存为 3 的图书：只有 3 个成功，其余 7 个抛异常，库存最终为 0")
    void should_only_3_threads_succeed_and_7_fail_when_10_threads_borrow_same_book_with_stock_3() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(TOTAL_THREADS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREADS);

        for (int i = 0; i < TOTAL_THREADS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    borrowService.borrowBook(USER_ID, BOOK_ID);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        Book book = bookMapper.findById(BOOK_ID);

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(failCount.get()).isEqualTo(TOTAL_THREADS - INITIAL_STOCK);
        assertThat(book.getAvailableStock()).isEqualTo(0);
        assertThat(exceptions).hasSize(TOTAL_THREADS - INITIAL_STOCK);
    }
}
