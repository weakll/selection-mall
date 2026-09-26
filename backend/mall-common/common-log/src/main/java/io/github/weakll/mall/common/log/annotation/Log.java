package io.github.weakll.mall.common.log.annotation;

import io.github.weakll.mall.common.log.enums.OperatorType;
import org.apache.poi.ss.usermodel.DataValidationConstraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log { // ⾃定义操作⽇志记录注解
    public String title() ; // 模块名称
    public OperatorType operatorType() default OperatorType.MANAGE;// 操作⼈类别
    public int businessType() ; // 业务类型（0其它 1新增 2修改 3删除）
    public boolean isSaveRequestData() default true; // 是否保存请求的参数
    public boolean isSaveResponseData() default true; // 是否保存响应的参数
}
