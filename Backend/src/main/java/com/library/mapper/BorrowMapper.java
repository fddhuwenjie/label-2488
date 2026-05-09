package com.library.mapper;

import com.library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 借阅记录数据访问层接口（MyBatis Mapper）
 *
 * <p>支持借阅记录的完整生命周期管理。
 *
 * <p><b>延迟加载说明：</b>
 * {@link #findById} 使用含延迟加载配置的 ResultMap，
 * 关联的 User 和 Book 对象仅在业务代码访问对应属性时才触发数据库查询。
 * {@link #findAll} 使用简单 ResultMap（不含关联），避免列表查询的 N+1 问题。
 */
public interface BorrowMapper {

    /**
     * 根据 ID 查询借阅记录详情（使用延迟加载 ResultMap）。
     * 关联的 user 和 book 在事务范围内按需加载
     *
     * @param id 借阅记录 ID
     * @return 借阅记录实体（含延迟加载的用户和图书），不存在时返回 null
     */
    BorrowRecord findById(@Param("id") Long id);

    /**
     * 分页查询借阅记录列表（使用简单 ResultMap，不含关联对象）
     *
     * @param userId 用户 ID，为 null 时查询所有用户记录（管理员使用）
     * @param status 借阅状态（BORROWED/RETURNED/OVERDUE），为 null 时查询所有状态
     * @return 借阅记录列表（配合 PageHelper 分页）
     */
    List<BorrowRecord> findAll(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 查询指定用户当前借阅中（BORROWED）的记录数量，用于判断借阅上限
     *
     * @param userId 用户 ID
     * @return 当前借阅中的记录数
     */
    int countActiveByUserId(@Param("userId") Long userId);

    /**
     * 新增借阅记录，执行后 borrowRecord.id 自动回填
     *
     * @param borrowRecord 借阅记录实体
     * @return 影响行数
     */
    int insert(BorrowRecord borrowRecord);

    /**
     * 动态更新借阅记录（主要用于还书更新 returnDate 和 status）
     *
     * @param borrowRecord 包含要更新字段的实体（id 不能为 null）
     * @return 影响行数
     */
    int update(BorrowRecord borrowRecord);

    /**
     * 查询所有已逾期未还的记录（due_date < 今天 且 status='BORROWED'）
     *
     * @return 逾期记录列表
     */
    List<BorrowRecord> findOverdue();
}
