package com.cg.quartz.task;

import com.cg.quartz.annotaion.CronExpression;
import com.cg.quartz.annotaion.HandleDissociate;
import com.cg.quartz.annotaion.Task;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.constant.em.HandleDissociateEnum;
import com.cg.quartz.entity.po.TaskPO;
import com.cg.quartz.service.ConfigRefreshService;
import com.cg.quartz.service.TaskStoreService;
import com.cg.quartz.utils.ObjectUtils;
import com.cg.quartz.utils.RandomUtils;
import com.cg.quartz.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置自动刷新任务
 * <p>持久化模式自动刷新所有定时任务corn表达式和启动状态, 内存模式下该任务停用</p>
 *
 * @author chunges
 * @version 1.0
 * @date 2020/8/12
 * @see com.cg.quartz.api.service.TaskManagerApiRmiService
 */
@Slf4j
@Task
@CronExpression(QuartzConstant.DEFAULT_CRON)
public class ConfigAutoRefreshTask {

    @Autowired(required = false)
    private TaskStoreService taskStoreService;

    @Autowired
    private ConfigRefreshService refreshService;

    public void run() {
        try {
            List<TaskPO> storeTasks = taskStoreService.list();
            for (TaskPO task : storeTasks) {
                refreshService.handleRefresh(task);
            }

            if (ObjectUtils.notEmpty(storeTasks) && canHandleDissociateTasks()) {
                refreshService.handleDissociateTasks(storeTasks);
            }
        } catch (Exception e) {
            log.error("[quartz], ConfigAutoRefreshTask execute catch a exception, caused by ==>", e);
        }
    }

    /**
     * 是否需要执行处理游离任务
     *
     * @return 执行处理游离任务(true:执行, false:不执行)
     */
    private boolean canHandleDissociateTasks(){
        String dissociateModel = SpringContextUtils.getApplicationContext().getEnvironment().getProperty(QuartzConstant.HANDLE_DISSOCIATE);
        if (ObjectUtils.isBlank(dissociateModel)) {
            return RandomUtils.getRandomProbability();
        }
        return HandleDissociateEnum.ALWAYS.name().equalsIgnoreCase(dissociateModel) || RandomUtils.getRandomProbability();
    }

}