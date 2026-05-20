package com.gym.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQL_SERVER));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 只为存在该字段的实体填充，避免 strictInsertFill 在缺字段时抛异常。
                // 项目里 Notification/Course/Payment 等多张表只有 created_at 没有 updated_at，
                // 之前用 strictInsertFill 会导致这些实体的 insert 直接失败。
                LocalDateTime now = LocalDateTime.now();
                if (metaObject.hasGetter("createdAt")) {
                    this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                }
                if (metaObject.hasGetter("updatedAt")) {
                    this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                if (metaObject.hasGetter("updatedAt")) {
                    this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                }
            }
        };
    }
}
