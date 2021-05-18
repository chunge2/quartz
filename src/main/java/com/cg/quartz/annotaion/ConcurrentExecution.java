package com.cg.quartz.annotaion;

import java.lang.annotation.*;

/**
 * 是否运行任务并发执行(默认不并发)
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Documented
@Inherited
public @interface ConcurrentExecution {
    /**
     * <p>是否允许并行执行</p>
     * <pre>
     *     实现Job接口任务如需要并发执行,@ConcurrentExecution(true), 或在任务类上<b>同时标注</b>@PersistJobDataAfterExecution ,@DisallowConcurrentExecution
     * </pre>
     * @see org.quartz.DisallowConcurrentExecution
     * @see org.quartz.PersistJobDataAfterExecution
     */
    boolean value() default false;
}
