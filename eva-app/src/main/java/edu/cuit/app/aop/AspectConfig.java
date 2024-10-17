package edu.cuit.app.aop;

import edu.cuit.domain.gateway.SemesterGateway;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AspectConfig {
    private final SemesterGateway semesterGateway;
    @Before("@annotation(edu.cuit.app.aop.CheckSemId)")
    public void checkSemId(JoinPoint joinPoint){
            Object[] args = joinPoint.getArgs();
            //找到参数名称为semId的参数
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();

            if (args != null && paramNames != null) {
                for (int i = 0; i < args.length; i++) {
                    if(paramNames[i].equals("semId")&& args[i] ==null){
                        args[i]=semesterGateway.getNow().getId();
                    }
                }
            }
    }

}
