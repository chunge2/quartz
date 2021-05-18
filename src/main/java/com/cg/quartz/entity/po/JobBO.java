package com.cg.quartz.entity.po;

import com.cg.quartz.constant.em.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.Trigger;

/**
 * @see TaskPO
 * @author chunge
 * @version 1.0
 * @date 2020/8/12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobBO {

    /**
     * 任务名
     */
    private String jobName;

    /**
     * job分组
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
     * 任务状态
     */
    private Trigger.TriggerState triggerState;

    /**
     * 任务状态(数据库状态)
     */
    private TaskStatus status;

    /**
     * 是否运行同一个任务并发执行即使上一个人任务未完成
     *
     * @see com.cg.quartz.annotaion.Task#allowConcurrent()
     * @see com.cg.quartz.annotaion.ConcurrentExecution
     */
    private Boolean allowConcurrent;

    /**
     * 任务描述
     */
    private String description;
}