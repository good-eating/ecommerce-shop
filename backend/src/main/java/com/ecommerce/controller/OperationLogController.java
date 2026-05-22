package com.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.Result;
import com.ecommerce.entity.OperationLog;
import com.ecommerce.mapper.OperationLogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/operation-logs")
@PreAuthorize("hasAnyRole('ADMIN','SALES')")
public class OperationLogController {

    private final OperationLogMapper operationLogMapper;

    public OperationLogController(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping
    public Result<List<OperationLog>> getOperationLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String resource,
            @RequestParam(defaultValue = "100") Integer limit) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt)
                .last("LIMIT " + limit);
        if (username != null && !username.isEmpty()) {
            wrapper.eq(OperationLog::getUsername, username);
        }
        if (operation != null && !operation.isEmpty()) {
            wrapper.eq(OperationLog::getOperation, operation);
        }
        if (resource != null && !resource.isEmpty()) {
            wrapper.eq(OperationLog::getResource, resource);
        }
        return Result.success(operationLogMapper.selectList(wrapper));
    }
}