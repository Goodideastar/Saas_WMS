package com.wms.aspect;

import com.wms.entity.OperationLog;
import com.wms.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogMapper operationLogMapper;

    @Pointcut("execution(* com.wms.controller.*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        OperationLog operationLog = new OperationLog();
        operationLog.setOperator("system");
        if (request != null) {
            operationLog.setIp(request.getRemoteAddr());
            operationLog.setRequestUrl(request.getRequestURI());
        }
        operationLog.setMethodName(
                joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            operationLog.setDuration(duration);
            operationLog.setStatus(1);
            operationLog.setResult("success");
            operationLogMapper.insert(operationLog);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            operationLog.setDuration(duration);
            operationLog.setStatus(0);
            operationLog.setResult(e.getMessage());
            operationLogMapper.insert(operationLog);
            throw e;
        }
    }
}
