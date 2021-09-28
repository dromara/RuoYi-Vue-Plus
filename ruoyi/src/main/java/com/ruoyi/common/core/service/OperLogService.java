package com.ruoyi.common.core.service;

import com.ruoyi.common.core.domain.dto.OperLogDTO;
import org.springframework.scheduling.annotation.Async;

public interface OperLogService {
    @Async
    void recordOper(OperLogDTO operLogDTO);
}
