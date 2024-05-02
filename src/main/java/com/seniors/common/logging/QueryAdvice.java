package com.seniors.common.logging;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class QueryAdvice implements MethodInterceptor{

    private final QueryCount queryCounter;
    public QueryAdvice(QueryCount queryCounter) {
        this.queryCounter = queryCounter;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        countPrepareStatement(invocation.getMethod());
        return invocation.proceed();
    }

    private void countPrepareStatement(Method method) {
        if (method.getName().equals("prepareStatement")) {
            queryCounter.increaseCount();
        }
    }
}
