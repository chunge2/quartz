package com.cg.quartz.annotaion;

import com.cg.quartz.log.TaskLogHandler;

import java.lang.annotation.*;

/**
 * 定时任务日志注解
 *
 * @author chunge
 * @version 1.0
 * @date 2020/12/22
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface TaskLog {
    /**
     * 日志等级
     */
    enum LogLevel{
        /**
         * 一般信息
         */
        INFO,
        /**
         * 警告
         */
        WARN,
        /**
         * 错误
         */
        ERROR
    }

    /**
     * 任务日志收集器
     */
    TaskLogHandler taskLog = TaskLogHandler.getInstance();
}