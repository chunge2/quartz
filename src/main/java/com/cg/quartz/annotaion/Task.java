package com.cg.quartz.annotaion;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 定时任务标识类
 * <pre>
 *     警告: 如果任务在实现Job接口同时标注了@Task注解, 则以Job接口优先
 * </pre>
 *
 * @author chunges
 * @version 1.0
 * @date 2020/8/12
 */

@Retention(RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Documented
@Inherited
@Component
public @interface Task {

    /**
     * 定时任务指定的方法,默认:run()方法
     */
    String method() default "run";

    /**
     * <p>是否允许并行执行</p>
     * <pre>
     *     false：定时任务ATask的run方法没执行完，即便下一次执行时间到了，也要等到本次目标方法执行完成后执行
     *     true： 允许任务并行执行, 即使上一次任务未执行完成
     * </pre>
     * @see ConcurrentExecution
     */
    boolean allowConcurrent() default false;

    /**
     * 定时任务表达式
     * <pre>
     *     优先级低于CronExpression
     * </pre>
     * @return cronExpression
     * @see CronExpression
     */
    String cron() default "";

    /**
     * 任务描述
     */
    String description() default "";
}