package com.ecommerce.service;

import com.ecommerce.entity.BehaviorLog;
import com.ecommerce.mapper.BehaviorLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class BehaviorLogServiceImpl implements BehaviorLogService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorLogServiceImpl.class);

    private final BehaviorLogMapper behaviorLogMapper;
    private static final int BATCH_SIZE = 100;
    private final ConcurrentLinkedQueue<BehaviorLog> behaviorLogQueue = new ConcurrentLinkedQueue<>();

    public BehaviorLogServiceImpl(BehaviorLogMapper behaviorLogMapper) {
        this.behaviorLogMapper = behaviorLogMapper;
    }

    @Override
    @Async
    public void asyncSaveLog(BehaviorLog behaviorLog) {
        try {
            behaviorLogQueue.offer(behaviorLog);

            if (behaviorLogQueue.size() >= BATCH_SIZE) {
                batchSaveLogs();
            }
        } catch (Exception e) {
            log.error("保存行为日志失败", e);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void batchSaveLogs() {
        List<BehaviorLog> logs = new ArrayList<>();
        BehaviorLog item;

        while ((item = behaviorLogQueue.poll()) != null && logs.size() < BATCH_SIZE) {
            logs.add(item);
        }

        if (!logs.isEmpty()) {
            try {
                if (logs.size() == 1) {
                    behaviorLogMapper.insert(logs.get(0));
                } else {
                    try {
                        behaviorLogMapper.batchInsert(logs);
                    } catch (Exception e) {
                        for (BehaviorLog behaviorLog : logs) {
                            try {
                                behaviorLogMapper.insert(behaviorLog);
                            } catch (Exception ex) {
                                log.error("插入单条行为日志失败", ex);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("批量保存行为日志失败", e);
            }
        }
    }
}