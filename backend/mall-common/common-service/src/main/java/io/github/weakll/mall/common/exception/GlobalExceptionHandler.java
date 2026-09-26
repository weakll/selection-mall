package io.github.weakll.mall.common.exception;

import io.github.weakll.mall.model.vo.common.Result;
import net.bytebuddy.asm.Advice;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    @ResponseBody
      public Result error(Exception e){
          e.printStackTrace();
          return Result.build(null , 201,"出现了异常") ;
   }
}
