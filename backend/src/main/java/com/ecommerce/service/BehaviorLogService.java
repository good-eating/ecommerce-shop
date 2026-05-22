package com.ecommerce.service;

import com.ecommerce.entity.BehaviorLog;

public interface BehaviorLogService {
    void asyncSaveLog(BehaviorLog behaviorLog);
    void batchSaveLogs();
}