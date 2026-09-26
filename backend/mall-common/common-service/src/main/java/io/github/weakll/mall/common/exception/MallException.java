package io.github.weakll.mall.common.exception;

import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import lombok.Data;

@Data
public class MallException extends RuntimeException{
    private Integer code;//错误状态码
    private String message;//错误信息
    private ResultCodeEnum resultCodeEnum;//封装状态错误信息

    public MallException(ResultCodeEnum resultCodeEnum){
        this.resultCodeEnum=resultCodeEnum;
        this.code = resultCodeEnum.getCode();
        this.message = resultCodeEnum.getMessage();

    }
    public MallException(Integer code,String message){
        this.code = code;
        this.message = message;

    }
}
