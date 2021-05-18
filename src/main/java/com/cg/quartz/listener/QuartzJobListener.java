package com.cg.quartz.listener;

import com.cg.quartz.api.service.TaskManagerApiRmiService;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.factory.QuartzJobFactory;
import com.cg.quartz.log.TaskLogHandler;
import com.cg.quartz.service.SchedulerManagerService;
import com.cg.quartz.service.TaskLogStoreService;
import com.cg.quartz.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * describe:定时任务监听器
 * <pre>
 *     监听spring容器初始化,初始化完成后调用onApplicationEvent,以实现注册所有Job/Task
 * </pre>
 *
 * @author chunges
 * @version 1.0
 * @date 2020-08-08
 */
@Slf4j
@Component
public class QuartzJobListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private SchedulerManagerService schedulerManager;

    @Autowired
    private TaskManagerApiRmiService taskApiRmiServer;

    @Autowired
    private QuartzJobFactory jobFactory;

    @Autowired(required = false)
    private TaskLogStoreService logStoreService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        long startTime = System.currentTimeMillis();
        schedulerManager.registerJobs();
        log.info("[quartz] all tasks and jobs load completed, consume {}ms", System.currentTimeMillis() - startTime);

        // JobFactory容器初始化
        jobFactory.initJobContainer();
        loadLogService();
        loadRmiService();
    }

    /**
     * 加载日志服务(默认异步日志)
     */
    private void loadLogService() {
        String asyncLog = SpringContextUtils.getApplicationContext().getEnvironment().getProperty(QuartzConstant.ENABLE_ASYNC_LOG, Boolean.TRUE.toString());
        TaskLogHandler.init(logStoreService, Boolean.parseBoolean(asyncLog));
    }

    /**
     * 加载rmi服务(默认关闭)
     */
    private void loadRmiService() {
        if (Boolean.parseBoolean(SpringContextUtils.getApplicationContext().getEnvironment().getProperty(QuartzConstant.ENABLE_RMI_FLAG))) {
            taskApiRmiServer.openRmiService();
        }
    }

}