package com.library.mapper;

import com.library.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图书数据访问层接口（MyBatis Mapper）
 *
 * <p>提供图书表的完整 CRUD 及库存管理操作。
 * 二级缓存已在 XML 中启用，缓存 5 分钟刷新一次。
 * 删除操作采用逻辑删除（status=0），保留借阅历史关联关系。
 */
public interface BookMapper {

    /**
     * 根据 ID 查询上架图书（status=1）
     *
     * @param id 图书 ID
     * @return 图书实体，不存在或已下架时返回 null
     */
    Book findById(@Param("id") Long id);

    /**
     * 分页查询图书列表，支持关键字和分类过滤
     *
     * @param keyword  搜索关键字，匹配书名/作者/ISBN，为 null 时不过滤
     * @param category 图书分类，为 null 时不过滤
     * @return 图书列表（配合 PageHelper 实现物理分页）
     */
    List<Book> findAll(@Param("keyword") String keyword, @Param("category") String category);

    /**
     * 新增图书，执行后 book.id 自动回填
     *
     * @param book 图书实体
     * @return 影响行数
     */
    int insert(Book book);

    /**
     * 动态更新图书信息（只更新非 null 字段）
     *
     * @param book 包含要更新字段的图书实体（id 不能为 null）
     * @return 影响行数
     */
    int update(Book book);

    /**
     * 逻辑删除图书（将 status 设为 0，下架但不物理删除）
     *
     * @param id 图书 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 减少可借数量（借书时调用）。
     * 仅在 available_stock > 0 时才执行，防止库存超借
     *
     * @param id 图书 ID
     * @return 影响行数（0 表示库存不足）
     */
    int decreaseAvailableStock(@Param("id") Long id);

    /**
     * 增加可借数量（还书时调用）。
     * 仅在 available_stock < total_stock 时才执行
     *
     * @param id 图书 ID
     * @return 影响行数
     */
    int increaseAvailableStock(@Param("id") Long id);

    /**
     * 统计上架图书总数（用于判断是否需要初始化测试数据）
     *
     * @return 图书总数
     */
    int count();

    /**
     * 根据 ISBN 查询图书（创建时去重校验）
     *
     * @param isbn ISBN 编号
     * @return 图书实体，不存在时返回 null
     */
    Book findByIsbn(@Param("isbn") String isbn);
}
