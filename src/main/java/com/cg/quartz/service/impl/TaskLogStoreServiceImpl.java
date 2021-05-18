package com.cg.quartz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cg.quartz.conf.TaskStoreConditional;
import com.cg.quartz.dao.TaskLogDao;
import com.cg.quartz.entity.po.TaskLogPO;
import com.cg.quartz.service.TaskLogStoreService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * 定时任务日志持久化服务实现
 *
 * @author chunge
 * @version 1.0
 * @date 2020/12/23
 */
@Service
@Conditional(TaskStoreConditional.class)
public class TaskLogStoreServiceImpl extends ServiceImpl<TaskLogDao, TaskLogPO> implements TaskLogStoreService {
}
