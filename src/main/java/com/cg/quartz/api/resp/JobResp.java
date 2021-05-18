package com.cg.quartz.api.resp;

import com.cg.quartz.constant.em.TaskStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务response bo
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/17
 */
@Setter
@Getter
@ToString
public class JobResp implements Serializable {
    /**
     * 任务名
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 触发器名
     */
    private String trigger;

    /**
     * 触发器分组
     */
    private String triggerGroup;

    /**
     * cron表达式
     */
    private String cronExpression;

    /**
     * 执行方法
     *
     * @see com.cg.quartz.annotaion.Task#method()
     */
    private String method;

    /**
     * 是否运行同一个任务并发执行即使上一个人任务未完成
     *
     * @see com.cg.quartz.annotaion.Task#allowConcurrent()
     * @see com.cg.quartz.annotaion.ConcurrentExecution
     */
    private Boolean allowConcurrent;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
