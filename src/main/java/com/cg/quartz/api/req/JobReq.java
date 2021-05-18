package com.cg.quartz.api.req;

import com.cg.quartz.constant.em.TaskStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 任务request bo
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/17
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class JobReq implements Serializable {
    /**
     * 任务名
     */
    private String jobName;

    /**
     * 任务分组
     *
     * @see com.cg.quartz.constant.QuartzConstant#DEFAULT_JOB_GROUP
     */
    private String jobGroup;

    /**
     * 触发器名
     */
    private String trigger;

    /**
     * 触发器分组
     *
     * @see com.cg.quartz.constant.QuartzConstant#DEFAULT_CRON_TRIGGER_GROUP
     */
    private String triggerGroup;

    /**
     * cron表达式
     */
    private String cronExpression;

    /**
     * 执行方法(仅针对@Task注解任务类)
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

    public JobReq(String jobName, String jobGroup) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
    }

    public JobReq(String trigger, String triggerGroup, String cronExpression) {
        this.trigger = trigger;
        this.triggerGroup = triggerGroup;
        this.cronExpression = cronExpression;
    }
}