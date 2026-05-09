package com.library.mapper;

import com.library.entity.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * BookMapper 集成测试
 *
 * <p>基于 H2 内存数据库，验证 MyBatis XML 中的 SQL 语句与实体映射是否正确。
 * 每个测试方法运行在独立事务中，结束后自动回滚，不污染其他测试用例。
 */
@SpringBootTest
@Transactional
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    // ===== insert & findById =====

    @Test
    @DisplayName("insert: 新增图书后 ID 自动回填，可通过 findById 查询到")
    void insert_and_findById() {
        Book book = buildBook("978-1-11111-111-1", "测试图书", "测试作者", 3);

        int rows = bookMapper.insert(book);
        assertThat(rows).isEqualTo(1);
        assertThat(book.getId()).isNotNull();

        Book found = bookMapper.findById(book.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("测试图书");
        assertThat(found.getAvailableStock()).isEqualTo(3);
    }

    // ===== findAll =====

    @Test
    @DisplayName("findAll: 无过滤条件时返回所有上架图书")
    void findAll_noFilter() {
        bookMapper.insert(buildBook("ISBN-A", "图书 A", "作者1", 2));
        bookMapper.insert(buildBook("ISBN-B", "图书 B", "作者2", 1));

        List<Book> books = bookMapper.findAll(null, null);
        assertThat(books.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findAll: 按关键字过滤时只返回匹配书名的图书")
    void findAll_keywordFilter() {
        bookMapper.insert(buildBook("ISBN-X", "Java 编程思想", "Bruce Eckel", 2));
        bookMapper.insert(buildBook("ISBN-Y", "Python 入门", "Guido", 1));

        List<Book> results = bookMapper.findAll("Java", null);
        assertThat(results).allMatch(b -> b.getTitle().contains("Java")
                || (b.getAuthor() != null && b.getAuthor().contains("Java")));
    }

    // ===== deleteById (逻辑删除) =====

    @Test
    @DisplayName("deleteById: 逻辑删除后 findById 返回 null")
    void deleteById_thenNotFound() {
        Book book = buildBook("ISBN-DEL", "将被下架的书", "作者", 1);
        bookMapper.insert(book);
        Long id = book.getId();

        assertThat(bookMapper.findById(id)).isNotNull();

        bookMapper.deleteById(id);

        assertThat(bookMapper.findById(id)).isNull();
    }

    // ===== library stock operations =====

    @Test
    @DisplayName("decreaseAvailableStock: 有库存时减 1 成功，返回影响行数 1")
    void decreaseAvailableStock_success() {
        Book book = buildBook("ISBN-DEC", "库存测试书", "作者", 2);
        bookMapper.insert(book);

        int affected = bookMapper.decreaseAvailableStock(book.getId());
        assertThat(affected).isEqualTo(1);

        Book updated = bookMapper.findById(book.getId());
        assertThat(updated.getAvailableStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("increaseAvailableStock: 归还后可借库存加 1")
    void increaseAvailableStock_success() {
        Book book = buildBook("ISBN-INC", "还书测试书", "作者", 2);
        bookMapper.insert(book);
        // 先借出一本
        bookMapper.decreaseAvailableStock(book.getId());

        bookMapper.increaseAvailableStock(book.getId());

        Book updated = bookMapper.findById(book.getId());
        assertThat(updated.getAvailableStock()).isEqualTo(2);
    }

    // ===== findByIsbn =====

    @Test
    @DisplayName("findByIsbn: ISBN 存在时返回对应图书，不存在时返回 null")
    void findByIsbn() {
        bookMapper.insert(buildBook("ISBN-UNIQUE", "唯一 ISBN 图书", "作者", 1));

        assertThat(bookMapper.findByIsbn("ISBN-UNIQUE")).isNotNull();
        assertThat(bookMapper.findByIsbn("ISBN-NONEXISTENT")).isNull();
    }

    // ===== helper =====

    private Book buildBook(String isbn, String title, String author, int stock) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setTotalStock(stock);
        book.setAvailableStock(stock);
        return book;
    }
}
