package com.tizhen.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入后台操作日志的方法。审计切面在业务事务内落库，失败时业务回滚。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 操作编码，例如 create / update / delete / enable / refresh_cf。 */
    String action();

    /** 目标类型，例如 route / problem / user / admin_user。 */
    String targetType() default "";
}
