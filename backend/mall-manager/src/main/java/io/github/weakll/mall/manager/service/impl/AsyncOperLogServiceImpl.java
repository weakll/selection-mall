package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.common.log.service.AsyncOperLogService;
import io.github.weakll.mall.manager.mapper.SysOperLogMapper;
import io.github.weakll.mall.model.entity.system.SysOperLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncOperLogServiceImpl implements AsyncOperLogService {
    @Autowired
    private SysOperLogMapper sysOperLogMapper;
    @Async // 异步执⾏保存⽇志操作
    @Override
    public void saveSysOperLog(SysOperLog sysOperLog) {
        sysOperLogMapper.insert(sysOperLog);
    }
}
