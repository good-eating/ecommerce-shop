package com.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.BehaviorLog;
import com.ecommerce.mapper.BehaviorLogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/logs")
@PreAuthorize("hasRole('SALES')")
public class BehaviorLogController {

    private final BehaviorLogMapper behaviorLogMapper;

    public BehaviorLogController(BehaviorLogMapper behaviorLogMapper) {
        this.behaviorLogMapper = behaviorLogMapper;
    }

    @GetMapping
    public Result<PageResult<BehaviorLog>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String path) {
        LambdaQueryWrapper<BehaviorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BehaviorLog::getCreatedAt);

        if (userId != null) {
            wrapper.eq(BehaviorLog::getUserId, userId);
        }
        if (path != null && !path.isEmpty()) {
            wrapper.like(BehaviorLog::getPath, path);
        }

        Page<BehaviorLog> logPage = behaviorLogMapper.selectPage(new Page<>(page, size), wrapper);

        List<BehaviorLog> items = logPage.getRecords().stream()
                .peek(log -> {
                    if (log.getParams() != null && log.getParams().length() > 200) {
                        log.setParams(log.getParams().substring(0, 200) + "...");
                    }
                })
                .collect(Collectors.toList());

        return Result.success(PageResult.of(logPage.getTotal(), items, (long) page, (long) size));
    }
}
