package com.xunfang.healthcare.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，记录关键写操作，满足需求文档模块十二的操作审计要求。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 所属模块，例如「患者管理」。
     */
    String module() default "";

    /**
     * 操作描述，例如「新增患者」。
     */
    String operation() default "";

    /**
     * 是否记录请求参数。
     */
    boolean saveParams() default true;
}
