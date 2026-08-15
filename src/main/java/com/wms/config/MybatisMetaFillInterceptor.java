package com.wms.config;

import com.wms.entity.BaseEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MybatisMetaFillInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object param = args[1];

        String msId = ms.getId();
        boolean isInsert = msId.contains(".insert");
        boolean isUpdate = msId.contains(".update");

        if ((isInsert || isUpdate) && param instanceof BaseEntity) {
            BaseEntity entity = (BaseEntity) param;
            LocalDateTime now = LocalDateTime.now();
            String operator = getCurrentOperator();

            if (isInsert) {
                entity.setCreateTime(now);
                entity.setUpdateTime(now);
                entity.setCreateBy(operator);
                entity.setUpdateBy(operator);
                entity.setIsDeleted(0);
                entity.setVersion(0);
            } else if (isUpdate && entity.getId() != null) {
                entity.setUpdateTime(now);
                entity.setUpdateBy(operator);
            }
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private String getCurrentOperator() {
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                Object principal = auth.getPrincipal();
                if (principal instanceof com.wms.security.UserDetailsImpl) {
                    return ((com.wms.security.UserDetailsImpl) principal).getUsername();
                }
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "system";
    }
}
