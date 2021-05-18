package com.cg.quartz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cg.quartz.conf.TaskStoreConditional;
import com.cg.quartz.dao.TaskStoreDao;
import com.cg.quartz.entity.po.TaskPO;
import com.cg.quartz.service.TaskStoreService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * 任务持久化服务实现
 *
 * @author chunge
 * @version 1.0
 * @date 2020/10/23
 */
@Service
@Conditional(TaskStoreConditional.class)
public class TaskStoreServiceImpl extends ServiceImpl<TaskStoreDao, TaskPO> implements TaskStoreService {
}