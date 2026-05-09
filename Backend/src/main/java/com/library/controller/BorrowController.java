package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.BorrowRequest;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.service.BorrowService;
import com.library.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅管理控制器
 *
 * <p>提供借书、还书、借阅记录查询等接口，是系统核心业务模块。
 *
 * <p>借阅规则：
 * <ul>
 *   <li>每位用户最多同时借阅 5 本图书</li>
 *   <li>每次借阅期限为 30 天，超期状态自动标记为 OVERDUE</li>
 *   <li>只有借阅人本人才能归还自己的图书</li>
 *   <li>管理员可查询所有用户的借阅记录，普通用户只能查自己的</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private UserService userService;

    /**
     * 借阅图书（POST /api/borrows）
     * 当前登录用户借阅指定 bookId 的图书
     *
     * <p>请求体示例：{@code {"bookId": 1}}
     *
     * @param request        包含 bookId 的请求体
     * @param authentication 当前认证信息（从中获取用户 ID）
     * @return 创建的借阅记录（含图书和用户信息）
     */
    @PostMapping
    public Result<BorrowRecord> borrowBook(@Valid @RequestBody BorrowRequest request,
                                           Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        BorrowRecord record = borrowService.borrowBook(currentUser.getId(), request.getBookId());
        return Result.success("借阅成功，请在到期前归还", record);
    }

    /**
     * 归还图书（PUT /api/borrows/{id}/return）
     * 只有借阅人本人才能归还，管理员代还功能通过 Admin 接口实现
     *
     * @param id             借阅记录 ID
     * @param authentication 当前认证信息
     * @return 更新后的借阅记录（status=RETURNED，含 returnDate）
     */
    @PutMapping("/{id}/return")
    public Result<BorrowRecord> returnBook(@PathVariable Long id,
                                           Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        BorrowRecord record = borrowService.returnBook(id, currentUser.getId());
        return Result.success("还书成功，感谢您使用校园图书馆", record);
    }

    /**
     * 查询借阅记录列表（GET /api/borrows）
     * 管理员查询所有用户的记录，普通用户自动过滤为仅查自己的记录
     *
     * @param status   借阅状态过滤（BORROWED/RETURNED/OVERDUE），可选
     * @param pageNum  页码，默认 1
     * @param pageSize 每页数量，默认 10
     * @return 分页借阅记录列表（不含详细的用户/图书对象，仅含 userId/bookId）
     */
    @GetMapping
    public Result<PageResult<BorrowRecord>> getBorrowRecords(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        // 管理员可查全部（userId=null），普通用户只能查自己
        Long userId = "ROLE_ADMIN".equals(currentUser.getRole()) ? null : currentUser.getId();
        PageResult<BorrowRecord> result = borrowService.getBorrowRecords(userId, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 查询当前用户的借阅记录（GET /api/borrows/my）
     * 语义更明确的当前用户借阅记录查询接口
     *
     * @param status   状态过滤（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 当前用户的分页借阅记录
     */
    @GetMapping("/my")
    public Result<PageResult<BorrowRecord>> getMyBorrows(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        PageResult<BorrowRecord> result =
                borrowService.getBorrowRecords(currentUser.getId(), status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 查询借阅记录详情（GET /api/borrows/{id}）
     * 此接口演示 MyBatis 延迟加载：返回的记录中包含完整的用户和图书信息，
     * 这些关联对象在 Service 层的 @Transactional 范围内通过延迟加载获取
     *
     * @param id             借阅记录 ID
     * @param authentication 当前认证信息（非管理员只能查自己的记录）
     * @return 包含完整用户和图书信息的借阅记录
     */
    @GetMapping("/{id}")
    public Result<BorrowRecord> getBorrowById(@PathVariable Long id,
                                              Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        BorrowRecord record = borrowService.getBorrowById(id);
        // 权限验证：非管理员只能查看自己的借阅记录
        if (!"ROLE_ADMIN".equals(currentUser.getRole())
                && !record.getUserId().equals(currentUser.getId())) {
            return Result.error(403, "无权查看他人的借阅记录");
        }
        return Result.success(record);
    }

    /**
     * 查询所有逾期未还记录（GET /api/borrows/overdue）
     * 仅管理员可访问，用于图书馆管理人员进行逾期提醒和处理
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页逾期记录列表
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<BorrowRecord>> getOverdueRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<BorrowRecord> result =
                borrowService.getBorrowRecords(null, "OVERDUE", pageNum, pageSize);
        return Result.success(result);
    }
}
