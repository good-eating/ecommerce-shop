package com.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserRole;
import com.ecommerce.mapper.RoleMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.mapper.UserRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserMapper userMapper, RoleMapper roleMapper,
                                UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<PageResult<User>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreatedAt);

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        if (roleCode != null && !roleCode.isEmpty()) {
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
            if (role != null) {
                List<UserRole> userRoles = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>()
                                .eq(UserRole::getRoleId, role.getId()));
                List<Long> userIds = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toList());
                if (!userIds.isEmpty()) {
                    wrapper.in(User::getId, userIds);
                } else {
                    wrapper.eq(User::getId, -1L);
                }
            }
        }

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        List<User> items = userPage.getRecords().stream().peek(u -> u.setPassword(null)).collect(Collectors.toList());

        return Result.success(PageResult.of(userPage.getTotal(), items, (long) page, (long) size));
    }

    @GetMapping("/roles")
    public Result<List<Role>> getRoles() {
        return Result.success(roleMapper.selectList(null));
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping("/{id}/roles")
    public Result<List<Role>> getUserRoles(@PathVariable Long id) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return Result.success(roles);
    }

    @PostMapping
    @Transactional
    public Result<User> createUser(@RequestBody User user) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
        User updated = userMapper.selectById(id);
        updated.setPassword(null);
        return Result.success(updated);
    }

    @PutMapping("/{id}/reset-password")
    @Transactional
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> deleteUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.disableUser(id);
        return Result.success();
    }

    @PostMapping("/{id}/roles")
    @Transactional
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(id);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return Result.success();
    }
}
