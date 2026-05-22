package com.ecommerce.service;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.entity.OperationLog;
import com.ecommerce.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Async
    public void recordOperation(Long userId, String username, String role, String operation,
                                 String resource, Long resourceId, String content,
                                 Map<String, Object> detail, HttpServletRequest request) {
        try {
            OperationLog opLog = new OperationLog();
            opLog.setUserId(userId);
            opLog.setUsername(username);
            opLog.setRole(role);
            opLog.setOperation(operation);
            opLog.setResource(resource);
            opLog.setResourceId(resourceId);
            opLog.setContent(content);
            opLog.setDetailJson(detail != null ? JSON.toJSONString(detail) : null);
            opLog.setIp(getClientIp(request));
            opLog.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            log.error("记录操作日志失败: userId={}, operation={}", userId, operation, e);
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