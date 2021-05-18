package com.cg.quartz.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.constant.em.TaskStatus;
import lombok.*;

import java.util.Date;

/**
 * 任务PO(用于获取具体任务信息)
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/17
 */
@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_quartz_task")
public class TaskPO {
    /**
     * pk
     */
    private Integer id;

    /**
     * 任务名
     */
    private String taskName;

    /**
     * cron表达式
     */
    private String cronExpression;

    /**
     * 执行方法
     * @see com.cg.quartz.annotaion.Task#method()
     */
    private String method;

    /**
     * 是否运行同一个任务并发执行即使上一个人任务未完成
     *
     * @see com.cg.quartz.annotaion.Task#allowConcurrent()
     * @see com.cg.quartz.annotaion.ConcurrentExecution
     */
    private Integer allowConcurrent;

    /**
     * 任务状态
     * <pre>
     *     是否启用任务
     * </pre>
     */
    private Integer status;

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

    public TaskPO(String taskName, String cronExpression, String description) {
        this.taskName = taskName;
        this.cronExpression = cronExpression;
        this.description = description;
        this.status = TaskStatus.ENABLE.getCode();
        this.method = QuartzConstant.EXECUTE_METHOD;
        this.allowConcurrent = TaskStatus.DISABLE.getCode();
    }
}