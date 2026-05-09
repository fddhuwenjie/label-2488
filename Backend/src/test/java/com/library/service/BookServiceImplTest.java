package com.library.service;

import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import com.library.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * BookServiceImpl 单元测试
 *
 * <p>使用 Mockito 隔离 BookMapper 依赖，纯粹验证 Service 层的业务逻辑分支。
 * 不启动 Spring 上下文，执行速度极快。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setIsbn("978-7-115-54742-4");
        sampleBook.setTitle("Spring Boot 实战");
        sampleBook.setAuthor("Craig Walls");
        sampleBook.setTotalStock(5);
        sampleBook.setAvailableStock(5);
        sampleBook.setStatus(1);
    }

    // ===== getBookById =====

    @Test
    @DisplayName("getBookById: 图书存在时正常返回")
    void getBookById_found() {
        given(bookMapper.findById(1L)).willReturn(sampleBook);

        Book result = bookService.getBookById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Boot 实战");
    }

    @Test
    @DisplayName("getBookById: 图书不存在时抛出 RuntimeException")
    void getBookById_notFound() {
        given(bookMapper.findById(99L)).willReturn(null);

        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("图书不存在");
    }

    // ===== createBook =====

    @Test
    @DisplayName("createBook: ISBN 重复时抛出 RuntimeException")
    void createBook_duplicateIsbn() {
        given(bookMapper.findByIsbn("978-7-115-54742-4")).willReturn(sampleBook);

        BookRequest request = buildRequest("978-7-115-54742-4", "另一本书", "作者");

        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ISBN 已存在");
    }

    @Test
    @DisplayName("createBook: ISBN 不重复时正常入库并返回完整图书信息")
    void createBook_success() {
        given(bookMapper.findByIsbn(anyString())).willReturn(null);
        // insert 后 findById 返回新图书
        given(bookMapper.findById(any())).willReturn(sampleBook);

        BookRequest request = buildRequest("978-0-13-468599-1", "Spring Boot 实战", "Craig Walls");
        request.setTotalStock(3);

        Book result = bookService.createBook(request);

        then(bookMapper).should().insert(any(Book.class));
        assertThat(result).isNotNull();
    }

    // ===== deleteBook =====

    @Test
    @DisplayName("deleteBook: 图书不存在时抛出 RuntimeException")
    void deleteBook_notFound() {
        given(bookMapper.findById(42L)).willReturn(null);

        assertThatThrownBy(() -> bookService.deleteBook(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("图书不存在");
        then(bookMapper).should(never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteBook: 图书存在时调用 deleteById")
    void deleteBook_success() {
        given(bookMapper.findById(1L)).willReturn(sampleBook);

        bookService.deleteBook(1L);

        then(bookMapper).should().deleteById(1L);
    }

    // ===== updateBook =====

    @Test
    @DisplayName("updateBook: 图书不存在时抛出 RuntimeException")
    void updateBook_notFound() {
        given(bookMapper.findById(99L)).willReturn(null);

        BookRequest request = buildRequest(null, "新书名", "作者");
        assertThatThrownBy(() -> bookService.updateBook(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("图书不存在");
    }

    // ===== helper =====

    private BookRequest buildRequest(String isbn, String title, String author) {
        BookRequest req = new BookRequest();
        req.setIsbn(isbn);
        req.setTitle(title);
        req.setAuthor(author);
        req.setTotalStock(3);
        return req;
    }
}
