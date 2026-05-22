package com.ecommerce.aspect;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.entity.BehaviorLog;
import com.ecommerce.security.UserDetailsImpl;
import com.ecommerce.service.BehaviorLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
public class BehaviorLogAspect {

    private static final Logger log = LoggerFactory.getLogger(BehaviorLogAspect.class);

    private final BehaviorLogService behaviorLogService;

    public BehaviorLogAspect(BehaviorLogService behaviorLogService) {
        this.behaviorLogService = behaviorLogService;
    }

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logBehavior(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentRequest();

        if (request == null) {
            return joinPoint.proceed();
        }

        BehaviorLog behaviorLog = new BehaviorLog();
        behaviorLog.setPath(request.getRequestURI());
        behaviorLog.setMethod(request.getMethod());
        behaviorLog.setIp(getClientIp(request));
        behaviorLog.setUserAgent(request.getHeader("User-Agent"));
        behaviorLog.setReferer(request.getHeader("Referer"));

        // 从路径中提取商品ID或分类ID
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        extractResourceIds(uri, queryString, behaviorLog);

        // 获取用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetailsImpl) {
                    behaviorLog.setUserId(((UserDetailsImpl) principal).getId());
                } else {
                    String username = authentication.getName();
                    behaviorLog.setUserId(Long.parseLong(username));
                }
            } catch (NumberFormatException e) {
                // 忽略非数字用户名
            }
        }

        // 参数脱敏处理
        behaviorLog.setParams(desensitizeParams(getRequestParams(joinPoint, request)));

        Object result;
        try {
            result = joinPoint.proceed();
            behaviorLog.setStatusCode(200);
        } catch (Exception e) {
            behaviorLog.setStatusCode(500);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            behaviorLog.setDuration((int) duration);
            behaviorLog.setCreatedAt(LocalDateTime.now());

            // 异步保存日志
            behaviorLogService.asyncSaveLog(behaviorLog);
        }

        return result;
    }

    private void extractResourceIds(String uri, String queryString, BehaviorLog behaviorLog) {
        // 匹配 /products/{id} 或 /products/{id}/xxx
        Pattern productPattern = Pattern.compile("/products?/(\\d+)");
        Matcher productMatcher = productPattern.matcher(uri);
        if (productMatcher.find()) {
            try {
                behaviorLog.setProductId(Long.parseLong(productMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        // 匹配 /categories/{id}
        Pattern categoryPattern = Pattern.compile("/categories?/(\\d+)");
        Matcher categoryMatcher = categoryPattern.matcher(uri);
        if (categoryMatcher.find()) {
            try {
                behaviorLog.setCategoryId(Long.parseLong(categoryMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        // 从查询参数中提取 categoryId
        if (queryString != null && queryString.contains("categoryId=")) {
            try {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.startsWith("categoryId=")) {
                        behaviorLog.setCategoryId(Long.parseLong(param.substring("categoryId=".length())));
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        // 从查询参数中提取 productId
        if (queryString != null && queryString.contains("productId=")) {
            try {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.startsWith("productId=")) {
                        behaviorLog.setProductId(Long.parseLong(param.substring("productId=".length())));
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        // 从请求体参数提取 - 需要过滤非路径相关路径
        if (uri.contains("/order") || uri.contains("/cart")) {
            return;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private String getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 过滤掉HttpServletRequest和HttpServletResponse对象
                Object[] filteredArgs = java.util.Arrays.stream(args)
                        .filter(arg -> !(arg instanceof HttpServletRequest))
                        .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                        .toArray();

                if (filteredArgs.length > 0) {
                    return JSON.toJSONString(filteredArgs);
                }
            }
            return JSON.toJSONString(request.getParameterMap());
        } catch (Exception e) {
            return "{}";
        }
    }

    private String desensitizeParams(String originalParams) {
        if (originalParams == null || originalParams.isEmpty()) {
            return originalParams;
        }

        // 脱敏敏感信息
        return originalParams
                .replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                .replaceAll("(?i)\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                .replaceAll("(?i)\"email\"\\s*:\\s*\"([^\"@]+)@", "\"email\":\"***@")
                .replaceAll("(?i)\"phone\"\\s*:\\s*\"\\d{3}(\\d{4})\\d{4}\"", "\"phone\":\"***$1****\"");
    }
}