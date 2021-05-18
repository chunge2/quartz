package com.cg.quartz.service;

import com.cg.quartz.entity.po.TaskPO;

import java.util.List;

/**
 * 刷新任务配置接口
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/5
 */
public interface ConfigRefreshService {
    /**
     * 处理任务刷新
     *
     * @param taskPo task pojo
     */
     void handleRefresh(TaskPO taskPo);

    /**
     * 处理游离任务
     *
     * @param storeTasks DB中Task
     * @see com.cg.quartz.annotaion.HandleDissociate
     */
    void handleDissociateTasks(List<TaskPO> storeTasks);
}