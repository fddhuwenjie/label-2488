package com.library.service;

import com.library.entity.Book;
import com.library.mapper.BookMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("BorrowService 并发集成测试")
class BorrowServiceConcurrencyTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookMapper bookMapper;

    @Test
    @DisplayName("should_只有3个成功且7个抛异常且库存最终为0_when_10个线程同时借阅库存为3的图书")
    @Sql(scripts = "classpath:sql/borrow-concurrency-init.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:sql/borrow-concurrency-cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void should_onlyThreeSucceedAndSevenFailAndStockZero_when_tenThreadsBorrowBookWithStockThree()
            throws InterruptedException {

        Long userId = 99L;
        Long bookId = 99L;
        int threadCount = 10;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long uid = userId;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    borrowService.borrowBook(uid, bookId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean allFinished = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(allFinished).isTrue();
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failCount.get()).isEqualTo(7);

        Book book = bookMapper.findById(bookId);
        assertThat(book).isNotNull();
        assertThat(book.getAvailableStock()).isEqualTo(0);
    }
}
