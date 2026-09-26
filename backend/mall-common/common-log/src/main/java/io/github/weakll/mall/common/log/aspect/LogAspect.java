package io.github.weakll.mall.common.log.aspect;

import io.github.weakll.mall.common.log.annotation.Log;
import io.github.weakll.mall.common.log.service.AsyncOperLogService;
import io.github.weakll.mall.common.log.utils.LogUtil;
import io.github.weakll.mall.model.entity.system.SysOperLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogAspect { // 环绕通知切⾯类定义
    @Autowired
    private AsyncOperLogService asyncOperLogService ;
    @Around(value = "@annotation(sysLog)")
    public Object doAroundAdvice(ProceedingJoinPoint joinPoint , Log sysLog) {
        // 构建前置参数
        SysOperLog sysOperLog = new SysOperLog() ;
        LogUtil.beforeHandleLog(sysLog , joinPoint , sysOperLog) ;
//        //控制台打印
//        String title = sysLog.title();
//        log.info("LogAspect...doAroundAdvice⽅法执⾏了"+title);
//        System.out.println("LogAspect...doAroundAdvice⽅法执⾏了"+title);
        Object proceed = null;
        try {
            proceed = joinPoint.proceed(); // 执⾏业务⽅法
            LogUtil.afterHandlLog(sysLog , proceed , sysOperLog , 0 , null) ;
        } catch (Throwable e) { // 代码执⾏进⼊到catch中，业务⽅法执⾏产⽣异常
//            throw new RuntimeException(e);
            e.printStackTrace();
            //打印异常信息
            LogUtil.afterHandlLog(sysLog , proceed , sysOperLog , 1 , e.getMessage()) ;
        }
        //保存日志信息
        asyncOperLogService.saveSysOperLog(sysOperLog);
        return proceed ; // 返回执⾏结果
    }
}
