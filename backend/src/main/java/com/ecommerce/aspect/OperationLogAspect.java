package com.ecommerce.aspect;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.entity.OperationLog;
import com.ecommerce.mapper.OperationLogMapper;
import com.ecommerce.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Around("execution(* com.ecommerce.controller.CategoryController.*(..)) || " +
            "execution(* com.ecommerce.controller.ProductController.updateProduct(..)) || " +
            "execution(* com.ecommerce.controller.ProductController.updateStatus(..)) || " +
            "execution(* com.ecommerce.controller.ProductController.createProduct(..)) || " +
            "execution(* com.ecommerce.controller.AdminUserController.*(..)) || " +
            "execution(* com.ecommerce.controller.SalesOrderController.updateStatus(..))")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 获取当前用户
        String username = "unknown";
        Long userId = null;
        String roleCode = "unknown";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) principal;
                username = userDetails.getUsername();
                userId = userDetails.getId();
                roleCode = userDetails.getAuthorities().stream()
                        .filter(a -> a.getAuthority().startsWith("ROLE_"))
                        .findFirst()
                        .map(a -> a.getAuthority().substring(5))
                        .orElse("unknown");
            }
        }

        // 提取操作类型
        String operation = extractOperation(methodName);
        String resource = extractResource(className);

        // 提取参数作为操作详情
        Map<String, Object> detail = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !(args[i] instanceof HttpServletRequest)
                        && !(args[i] instanceof jakarta.servlet.http.HttpServletResponse)) {
                    detail.put("arg" + i, args[i]);
                }
            }
        }

        Long resourceId = extractResourceId(args, request);

        Object result = joinPoint.proceed();

        String content = buildContent(className, methodName, resourceId, resource, operation, args);

        try {
            OperationLog opLog = new OperationLog();
            opLog.setUserId(userId);
            opLog.setUsername(username);
            opLog.setRole(roleCode);
            opLog.setOperation(operation);
            opLog.setResource(resource);
            opLog.setResourceId(resourceId);
            opLog.setContent(content);
            opLog.setDetailJson(detail.isEmpty() ? null : JSON.toJSONString(detail));
            opLog.setIp(getClientIp(request));
            opLog.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }

        return result;
    }

    private String extractOperation(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("add") || methodName.startsWith("insert")) {
            return "create";
        } else if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")) {
            return "update";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "delete";
        } else if (methodName.startsWith("reset")) {
            return "reset";
        }
        return methodName;
    }

    private String extractResource(String className) {
        if (className.contains("Category")) return "category";
        if (className.contains("Product")) return "product";
        if (className.contains("Order")) return "order";
        if (className.contains("User")) return "user";
        if (className.contains("Sales")) return "sales";
        return className.replace("Controller", "").toLowerCase();
    }

    private Long extractResourceId(Object[] args, HttpServletRequest request) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long) {
                    return (Long) arg;
                }
                if (arg instanceof Integer) {
                    return ((Integer) arg).longValue();
                }
            }
        }
        // 从URL路径中提取
        if (request != null) {
            String uri = request.getRequestURI();
            String[] parts = uri.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                try {
                    return Long.parseLong(parts[i]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private String buildContent(String className, String methodName, Long resourceId,
                                 String resource, String operation, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(operation).append(" ").append(resource);
        if (resourceId != null) {
            sb.append(" [id=").append(resourceId).append("]");
        }
        return sb.toString();
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
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
}