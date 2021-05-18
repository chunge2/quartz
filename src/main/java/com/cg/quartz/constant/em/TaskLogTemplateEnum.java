package com.cg.quartz.constant.em;

/**
 * 任务日志模版枚举
 *
 * @author chunge
 * @version 1.0
 * @date 2020/12/23
 */
public enum TaskLogTemplateEnum {
    /**
     * 简单模式(不需要填充参数)
     */
    SIMPLE,
    /**
     * 更多(填充多个参数)
     */
    MORE,
    /**
     * 异常(异常类型)
     */
    EXCEPTION;
}