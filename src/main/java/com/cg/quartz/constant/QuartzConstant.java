package com.cg.quartz.constant;

/**
 * 常量类(存放预设常量)
 *
 * @author chuges
 * @version 1.0
 * @date 2020/8/13
 */
public class QuartzConstant {

    /**
     * 默认Job分组
     */
    public static final String DEFAULT_JOB_GROUP = "DEFAULT_JOB_GROUP";

    /**
     * 默认cronTrigger分组
     */
    public static final String DEFAULT_CRON_TRIGGER_GROUP = "DEFAULT_CRON_TRIGGER_GROUP";

    /**
     * Task注解 默认执行的方法
     */
    public static final String EXECUTE_METHOD = "run";

    /**
     * 配置自动刷新定时任务
     */
    public static final String CONFIG_AUTO_REFRESH_TASK = "configAutoRefreshTask";

    /**
     * 默认表达式
     */
    public static final String DEFAULT_CRON = "0/6 * * * * ?";

    /**
     * 空字符串
     */
    public static final String EMPTY_STRING = "";

    /**
     * main方法
     */
    public static final String MAIN = "main";

    /**
     * 是否需要开启定时任务标识(命令行配置)
     */
    public static final String RUN_TASK_COMMAND_LINE_FLAG = "runtask";

    /**
     * 开启定时任务标识(spring config)
     */
    public static final String RUN_TASK_CONFIG_FLAG = "quartz.run-task";

    /**
     * 启用持久化标识(spring config)
     */
    public static final String ENABLE_PERSIST_FLAG = "quartz.enable-persist";

    /**
     * 启用rmi标识(spring config)
     */
    public static final String ENABLE_RMI_FLAG = "quartz.enable-rmi";

    /**
     * rmi主题(spring config)
     */
    public static final String RMI_TOPIC = "quartz.rmi-topic";

    /**
     * rmi端口(spring config)
     * @see java.rmi.registry.Registry#REGISTRY_PORT
     */
    public static final String RMI_PORT = "quartz.rmi-port";

    /**
     * 启用异步日志标识(spring config)
     */
    public static final String ENABLE_ASYNC_LOG = "quartz.enable-async-log";

    /**
     * 处理游离任务标识(spring config)
     */
    public static final String HANDLE_DISSOCIATE = "quartz.handle-dissociate";

    /**
     * 解析日志模版错误
     */
    public static final String ERROR_PARSE_LOG = "Parse Log Template Error";

    /**
     * 异常日志头
     */
    public static final String ERROR_LOG_MARK_HEAD = "[Exception]";

    /**
     * 0
     */
    public static final int ZERO = 0;

    /**
     * 1
     */
    public static final int ONE = 1;

    /**
     * 2
     */
    public static final int TWO = 2;
}