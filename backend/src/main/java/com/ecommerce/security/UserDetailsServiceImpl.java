package com.ecommerce.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserRole;
import com.ecommerce.mapper.RoleMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.mapper.UserRoleMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public UserDetailsServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getStatus, 1)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 获取用户角色
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, user.getId())
        );

        String roleCode = "CUSTOMER"; // 默认角色
        if (userRole != null) {
            Role role = roleMapper.selectById(userRole.getRoleId());
            if (role != null) {
                roleCode = role.getCode();
            }
        }

        return new UserDetailsImpl(user, roleCode);
    }
}