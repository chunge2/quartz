package com.cg.quartz.annotaion;

import java.lang.annotation.*;

/**
 * @author chunges
 * @version 1.0
 * @date 2020/8/13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Documented
@Inherited
public @interface CronExpression {

    /**
     * cron表达式
     * <pre>
     *     内存模式下实现Job接口任务必须使用此注解配置cron表达式
     *     内存模式下优先级最高
     * </pre>
     *
     * @return cronExpression
     * @see Task#cron()
     */
    String value() default "";
}
