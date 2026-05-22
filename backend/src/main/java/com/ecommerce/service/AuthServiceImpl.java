package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserDTO;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserRole;
import com.ecommerce.mapper.RoleMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.mapper.UserRoleMapper;
import com.ecommerce.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginLogService loginLogService;
    private final HttpServletRequest request;

    public AuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, LoginLogService loginLogService, HttpServletRequest request) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.loginLogService = loginLogService;
        this.request = request;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String ip = loginLogService.getClientIp(this.request);
        String userAgent = this.request.getHeader("User-Agent");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, request.getUsername())
            );

            // 获取用户角色
            String roleCode = "CUSTOMER";
            UserRole userRole = userRoleMapper.selectOne(
                    new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, user.getId())
            );
            if (userRole != null) {
                Role role = roleMapper.selectById(userRole.getRoleId());
                if (role != null) {
                    roleCode = role.getCode();
                }
            }

            String accessToken = jwtService.generateToken((UserDetails) authentication.getPrincipal());
            String refreshToken = jwtService.generateToken((UserDetails) authentication.getPrincipal());

            // 记录登录成功日志
            loginLogService.recordLogin(user.getId(), user.getUsername(), roleCode, ip, userAgent, true, null);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .user(convertToDTO(user))
                    .build();
        } catch (BadCredentialsException e) {
            loginLogService.recordLogin(null, request.getUsername(), null, ip, userAgent, false, "密码错误");
            throw new BadCredentialsException("用户名或密码错误");
        } catch (DisabledException e) {
            loginLogService.recordLogin(null, request.getUsername(), null, ip, userAgent, false, "账号已禁用");
            throw new DisabledException("账号已被禁用");
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, request.getEmail())
        );
        if (existingUser != null) {
            throw new BusinessException("邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAge(request.getAge());
        user.setGender(request.getGender());
        user.setCity(request.getCity());
        user.setStatus(1);
        userMapper.insert(user);

        // 分配默认角色（CUSTOMER）
        Role customerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getCode, "CUSTOMER")
        );
        if (customerRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(customerRole.getId());
            userRoleMapper.insert(userRole);
        }

        // 生成token
        String accessToken = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        java.util.Collections.emptyList()
                )
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(convertToDTO(user))
                .build();
    }

    @Override
    public void logout(String token) {
        try {
            jwtService.blacklistToken(token);
        } catch (Exception e) {
            // redis不可用时，忽略黑名单错误，前端清除token即完成登出
        }
    }

    @Override
    public AuthResponse refreshToken(String token) {
        String newToken = jwtService.refreshToken(token);
        String username = jwtService.extractUsername(token);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        return AuthResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(convertToDTO(user))
                .build();
    }

    @Override
    public UserDTO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        if (userDTO.getAge() != null) {
            user.setAge(userDTO.getAge());
        }
        if (userDTO.getGender() != null) {
            user.setGender(userDTO.getGender());
        }
        if (userDTO.getCity() != null) {
            user.setCity(userDTO.getCity());
        }
        if (userDTO.getAvatar() != null) {
            user.setAvatar(userDTO.getAvatar());
        }
        userMapper.updateById(user);
        return convertToDTO(user);
    }

    private UserDTO convertToDTO(User user) {
        // 获取用户角色
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, user.getId())
        );

        String roleCode = "CUSTOMER";
        if (userRole != null) {
            Role role = roleMapper.selectById(userRole.getRoleId());
            if (role != null) {
                roleCode = role.getCode();
            }
        }

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .age(user.getAge())
                .gender(user.getGender())
                .city(user.getCity())
                .avatar(user.getAvatar())
                .role(roleCode)
                .createdAt(user.getCreatedAt())
                .build();
    }
}