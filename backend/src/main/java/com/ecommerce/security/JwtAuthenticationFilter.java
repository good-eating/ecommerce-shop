package com.ecommerce.security;

import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                    RedisTemplate<String, Object> redisTemplate, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("JWT过滤: 无Authorization头, path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            log.error("JWT过滤: 解析token失败", e);
            filterChain.doFilter(request, response);
            return;
        }

        if (userEmail == null) {
            log.info("JWT过滤: token中用户名为空");
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.info("JWT过滤: 已认证,跳过");
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = this.userDetailsService.loadUserByUsername(userEmail);
        } catch (Exception e) {
            log.error("JWT过滤: 加载用户失败", e);
            filterChain.doFilter(request, response);
            return;
        }

        boolean tokenValid;
        try {
            tokenValid = jwtService.isTokenValid(jwt, userDetails);
        } catch (Exception e) {
            log.error("JWT过滤: token验证异常", e);
            filterChain.doFilter(request, response);
            return;
        }

        if (!tokenValid) {
            log.info("JWT过滤: token无效");
            filterChain.doFilter(request, response);
            return;
        }

        // 检查用户是否被强制登出
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, userEmail)
        );
        if (user == null) {
            log.info("JWT过滤: 用户不存在");
            filterChain.doFilter(request, response);
            return;
        }

        boolean isBlacklisted = false;
        try {
            isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:user:" + user.getId()));
        } catch (Exception ignored) {}
        if (isBlacklisted) {
            log.info("JWT过滤: 用户被禁用");
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.info("JWT过滤: 认证成功, user={}, authorities={}", userEmail, userDetails.getAuthorities());

        filterChain.doFilter(request, response);
    }
}