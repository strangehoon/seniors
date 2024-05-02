package com.seniors.common.logging;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.stereotype.Component;
import java.sql.Connection;

@Component
@Aspect
@RequiredArgsConstructor
public class DatasourceAspect {
    private final QueryCount queryCounter;

    @Around("execution(* javax.sql.DataSource.getConnection(..))")
    public Connection getConnection(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Connection connection = (Connection) proceedingJoinPoint.proceed();
        ProxyFactory proxyFactory = new ProxyFactory(connection);
        proxyFactory.addAdvice(new QueryAdvice(queryCounter));
        return (Connection) proxyFactory.getProxy();
    }
}
