package com.library.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.List;

/**
 * 分页查询结果封装类
 *
 * <p>基于 PageHelper 的 {@link PageInfo} 构建，提供统一的分页数据结构。
 * 所有分页查询接口均返回此类型，确保前端分页参数格式统一。
 *
 * @param <T> 分页列表中元素的类型
 */
@Data
public class PageResult<T> {

    /** 当前页码（从 1 开始） */
    private int pageNum;

    /** 每页记录数 */
    private int pageSize;

    /** 总记录数 */
    private long total;

    /** 总页数 */
    private int pages;

    /** 当前页的数据列表 */
    private List<T> list;

    /**
     * 从 PageHelper 的 {@link PageInfo} 对象构建分页结果。
     *
     * <p>使用方式：在 Service 层调用 {@code PageHelper.startPage()} 后执行查询，
     * 将查询结果包装为 PageInfo，再通过此方法转换为统一的 PageResult 格式。
     *
     * @param pageInfo PageHelper 分页信息对象
     * @param <T>      数据列表元素类型
     * @return 统一格式的分页结果
     */
    public static <T> PageResult<T> of(PageInfo<T> pageInfo) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        result.setList(pageInfo.getList());
        return result;
    }
}
