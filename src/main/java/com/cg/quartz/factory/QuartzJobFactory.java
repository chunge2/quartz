package com.cg.quartz.factory;

import com.cg.quartz.annotaion.Task;
import com.cg.quartz.annotaion.TaskLog;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.utils.ObjectUtils;
import com.cg.quartz.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.cg.quartz.annotaion.TaskLog.taskLog;

/**
 * 定时任务工厂
 * <pre>
 *     每次执行Job前会调用createJobInstance方法创建Job实例(实际上是注册JobDetail实例到Scheduler)
 * </pre>
 *
 * @author chunges
 * @version 1.0
 * @date 2020-08-08
 */
@Slf4j
@TaskLog
@Component
public class QuartzJobFactory extends AdaptableJobFactory {

    /**
     * task任务实例缓存(Task注解所需要的Job由JobFactory创建)
     */
    private Map<String, Object> taskCacheContainer;

    /**
     * job容器(实现Job接口实例)
     */
    private Map<String, Job> jobInstanceContainer;

    /**
     * task实例(从Spring获取已加载@Task实例)
     */
    private Map<String, Object> taskInstanceContainer;

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        try {
            String jobName = bundle.getJobDetail().getKey().getName();
            Object job = jobInstanceContainer.get(jobName);
            boolean isJobInstanceEmpty;
            if (isJobInstanceEmpty = ObjectUtils.isNull(job)) {
                job = taskCacheContainer.get(jobName);
                if (ObjectUtils.isNull(job)) {
                    // @Task注解任务类需要的JodDetail
                    job = super.createJobInstance(bundle);
                    taskCacheContainer.put(jobName, job);
                }
            }
            recordJobInvoker(jobName, job, isJobInstanceEmpty);
            return job;
        } catch (Exception e) {
            log.error("[quartz], createJobInstance catch a exception(ignore it, task still will be created), name={}, caused by==>",
                    bundle.getJobDetail().getKey().getName(), e);
            return super.createJobInstance(bundle);
        }
    }

    /**
     * 初始化Jo容器, 用于构建单例Job
     */
    public void initJobContainer() {
        log.info("[quartz], init job container(include implement Job interface) and job instances(include present @Task annotation)");
        if (ObjectUtils.isEmpty(jobInstanceContainer)) {
            jobInstanceContainer = SpringContextUtils.getBeansOfType(Job.class);
        }
        if (ObjectUtils.isEmpty(taskCacheContainer)) {
            taskInstanceContainer = SpringContextUtils.getBeansWithAnnotation(Task.class);
            int initCapacity = taskInstanceContainer.size();
            taskCacheContainer = new HashMap<>(initCapacity == 0 ? 16 : initCapacity);
        }
    }

    /**
     * 记录任务调度记录
     *
     * @param jobName            job name
     * @param job                job instance
     * @param isJobInstanceEmpty 从job容器获取是否为空
     */
    private void recordJobInvoker(String jobName, Object job, boolean isJobInstanceEmpty) {
        Object targetJob = isJobInstanceEmpty ? taskInstanceContainer.getOrDefault(jobName, QuartzConstant.EMPTY_STRING) : job;
        taskLog.onlyWrite("invoker:" + jobName, targetJob.getClass().getName());
    }
}