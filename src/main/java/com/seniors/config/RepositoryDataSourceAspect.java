package com.seniors.config;

import com.seniors.common.annotation.Database;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class RepositoryDataSourceAspect {


    @Pointcut("execution(* *..*Service.*(..))")
    private void serviceMethods() {
    }

    @Around("serviceMethods() && @annotation(database)")
    public Object handler(ProceedingJoinPoint joinPoint, Database database) throws Throwable {
        try {
            DataSourceHolder.setDatabaseType(database.value());
            Object returnType = joinPoint.proceed();
            return returnType;
        } finally {
            DataSourceHolder.clearDatabaseType();
        }
    }
}

