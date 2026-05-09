package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.PageResult;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BookController MVC 测试
 *
 * <p>启动完整 Spring 上下文（含 Security 过滤链），使用 @MockBean 隔离 BookService，
 * 通过 MockMvc 发送 HTTP 请求并断言响应状态码与 JSON 结构。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

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

    // ===== GET /api/books =====

    @Test
    @DisplayName("GET /api/books: 已登录用户可获取分页图书列表")
    @WithMockUser
    void getAllBooks_authenticated() throws Exception {
        PageResult<Book> page = new PageResult<>();
        page.setPageNum(1);
        page.setPageSize(10);
        page.setTotal(1);
        page.setPages(1);
        page.setList(List.of(sampleBook));
        given(bookService.getAllBooks(any(), any(), anyInt(), anyInt())).willReturn(page);

        mockMvc.perform(get("/api/books")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].title").value("Spring Boot 实战"));
    }

    @Test
    @DisplayName("GET /api/books: 未登录时返回 401")
    void getAllBooks_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    // ===== GET /api/books/{id} =====

    @Test
    @DisplayName("GET /api/books/{id}: 图书存在时返回详情")
    @WithMockUser
    void getBookById_found() throws Exception {
        given(bookService.getBookById(1L)).willReturn(sampleBook);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Spring Boot 实战"));
    }

    @Test
    @DisplayName("GET /api/books/{id}: Service 抛出异常时返回 400")
    @WithMockUser
    void getBookById_notFound() throws Exception {
        given(bookService.getBookById(99L))
                .willThrow(new RuntimeException("图书不存在或已下架：ID=99"));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ===== POST /api/books =====

    @Test
    @DisplayName("POST /api/books: ADMIN 可新增图书，返回 200 及图书信息")
    @WithMockUser(roles = "ADMIN")
    void createBook_asAdmin() throws Exception {
        given(bookService.createBook(any())).willReturn(sampleBook);

        BookRequest request = new BookRequest();
        request.setTitle("Spring Boot 实战");
        request.setAuthor("Craig Walls");
        request.setTotalStock(5);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot 实战"));
    }

    @Test
    @DisplayName("POST /api/books: 普通用户新增图书时返回 403")
    @WithMockUser(roles = "USER")
    void createBook_asUser_forbidden() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("Spring Boot 实战");
        request.setAuthor("Craig Walls");
        request.setTotalStock(5);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/books: 请求体缺少必填字段时返回 400")
    @WithMockUser(roles = "ADMIN")
    void createBook_invalidRequest() throws Exception {
        // title 为必填项，此处故意置空触发 @NotBlank 校验
        BookRequest request = new BookRequest();
        request.setTotalStock(5);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE /api/books/{id} =====

    @Test
    @DisplayName("DELETE /api/books/{id}: ADMIN 下架图书成功，返回 200")
    @WithMockUser(roles = "ADMIN")
    void deleteBook_asAdmin() throws Exception {
        willDoNothing().given(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("图书已下架"));
    }
}
