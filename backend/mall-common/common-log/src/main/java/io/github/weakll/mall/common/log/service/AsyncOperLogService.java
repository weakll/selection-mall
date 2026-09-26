package io.github.weakll.mall.common.log.service;

import io.github.weakll.mall.model.entity.system.SysOperLog;

public interface AsyncOperLogService {
    void saveSysOperLog(SysOperLog sysOperLog);
}
