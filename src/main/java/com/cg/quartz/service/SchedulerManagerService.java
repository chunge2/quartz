package com.cg.quartz.service;

import com.cg.quartz.entity.po.JobBO;

import java.util.List;

/**
 * Quartz管理服务(内部使用)
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/12
 */
public interface SchedulerManagerService {

    /**
     * 注册(激活)所有Job
     */
    void registerJobs();

    /**
     * 获取Job信息
     *
     * @param jobName job name
     * @param jobGroup job group
     * @return Job信息(默认返回null)
     */
    JobBO getJob(String jobName, String jobGroup);

    /**
     * 获取所有Job信息

     * @return Job信息列表
     */
    List<JobBO> listJobs();

    /**
     * 立即触发Job
     *
     * @param jobName  job name
     * @param jobGroup job group
     */
    void runJobNow(String jobName, String jobGroup);

    /**
     * 看下QuartzManager#addJob() 动态添加Task(Task必须存在)
     *
     * @param jobName     job name/ task name
     * @param jobInstance job实例(Job接口实例, 持有@Task注解实例)
     */
    void addJob(String jobName, Object jobInstance);

    /**
     * 暂停任务
     *
     * @param jobName  job name
     * @param jobGroup job group
     */
    void pauseJob(String jobName, String jobGroup);

    /**
     * 暂停所有任务
     */
    void pauseAll();

    /**
     * 恢复任务
     *
     * @param jobName  job name
     * @param jobGroup job group
     */
    void resumeJob(String jobName, String jobGroup);

    /**
     * 恢复所有任务
     */
    void resumeAll();

    /**
     * 修改任务执行时间
     *
     * @param triggerName    trigger name
     * @param triggerGroup   trigger group
     * @param cronExpression cron expression
     * @return 修改结果(true : 修改成功 ; false : 修改失败)
     */
    boolean modifyJob(String triggerName, String triggerGroup, String cronExpression);

    /**
     * 删除某个任务
     *
     * @param jobName  job name
     * @param jobGroup job group
     * @return 删除结果 (true:成功;false:失败)
     */
    boolean deleteJob(String jobName, String jobGroup);

    /**
     * job是否被调度器scheduler加载
     *
     * @param jobName  job name
     * @param jobGroup job group
     * @return 加载结果(true:已加载;false:未加载)
     */
    boolean isLoaded(String jobName, String jobGroup);

    /**
     * 获取持久化状态
     *
     * @return persistenceStatus(true:启用持久化; false:未启用持久化)
     */
    boolean getPersistenceStatus();
}