package com.cg.quartz.service.impl;

import com.cg.quartz.annotaion.Task;
import com.cg.quartz.annotaion.TaskLog;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.constant.em.TaskStatus;
import com.cg.quartz.entity.po.JobBO;
import com.cg.quartz.entity.po.TaskPO;
import com.cg.quartz.service.ConfigRefreshService;
import com.cg.quartz.service.SchedulerManagerService;
import com.cg.quartz.utils.Assert;
import com.cg.quartz.utils.ObjectUtils;
import com.cg.quartz.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cg.quartz.annotaion.TaskLog.taskLog;

/**
 * 刷新任务配置实现
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/5
 */
@Slf4j
@Service
public class ConfigRefreshServiceImpl implements ConfigRefreshService {

    @Autowired
    private SchedulerManagerService schedulerService;

    @Override
    public void handleRefresh(TaskPO storeTask) {
        try {
            // 任务已加载, 关闭, 开启或更新表达式
            JobBO currentJob = schedulerService.getJob(storeTask.getTaskName(), QuartzConstant.DEFAULT_JOB_GROUP);
            if (ObjectUtils.notNull(currentJob)) {
                boolean sameCron = currentJob.getCronExpression().equals(storeTask.getCronExpression().trim());
                if (sameCron && currentJob.getStatus().getCode() == storeTask.getStatus()) {
                    return;
                }

                // 任务已启用, cron表达式不同, 更新任务表达式(仅更新加载的任务)
                boolean taskEnable = TaskStatus.ENABLE.getCode() == storeTask.getStatus() || TaskStatus.ENABLE == currentJob.getStatus();
                if (taskEnable && !sameCron) {
                    log.debug("[quartz], handleRefresh update cron, before cron={}, current cron={}, task={}",
                            currentJob.getCronExpression(), storeTask.getCronExpression(), storeTask.getTaskName());
                    schedulerService.modifyJob(storeTask.getTaskName(), QuartzConstant.DEFAULT_CRON_TRIGGER_GROUP, storeTask.getCronExpression());
                }

                // 同步DB任务状态到RAM任务状态(以DB状态为准 单向同步)
                toggleTask(storeTask.getStatus(), currentJob.getStatus(), storeTask.getTaskName());
                return;
            }

            // 任务未加载(且数据库是启用状态), scheduler加载任务
            String taskName = storeTask.getTaskName();
            Object taskInstance = Optional.ofNullable(SpringContextUtils.getBeansWithAnnotation(Task.class).get(taskName))
                    .orElseGet(() -> SpringContextUtils.getBeansOfType(Job.class).get(taskName));
            if (TaskStatus.ENABLE.getCode() == storeTask.getStatus()) {
                log.info("[quartz], add job, job name={}, job instance={}", taskName, taskInstance);
                schedulerService.addJob(taskName, taskInstance);
            }
        } catch (Exception e) {
            log.error("[quartz], handleRefresh catch a exception, StackTrace ==>", e);
        }
    }

    @Override
    public void handleDissociateTasks(List<TaskPO> storeTasks) {
        // 遍历内存任务, 剔除内存中存在, DB不存在的任务
        Map<String, TaskPO> storeTaskMap = storeTasks.stream().collect(Collectors.toMap(TaskPO::getTaskName, Function.identity()));
        schedulerService.listJobs().forEach(t -> {
            // 自动刷新任务跳过任务跳过
            if (ObjectUtils.isNull(storeTaskMap.get(t.getJobName())) && !QuartzConstant.CONFIG_AUTO_REFRESH_TASK.equals(t.getJobName())) {
                log.info("[quartz], delete dissociate task, jobName={}, jobGroup={}", t.getJobName(), t.getJobGroup());
                schedulerService.deleteJob(t.getJobName(), t.getJobGroup());
            }
        });
    }

    /**
     * 开启或关闭任务
     * <pre>
     *     数据库任务开启, 当前任务关闭 => 恢复任务
     *     数据库任务关闭, 当前任务开启 => 暂停任务
     * </pre>
     *
     * @param storeTaskStatus  数据库任务状态
     * @param currentJobStatus 当前任务(RAM中任务)状态
     * @param jobName          任务名
     */
    private void toggleTask(Integer storeTaskStatus, TaskStatus currentJobStatus, String jobName) {
        if (TaskStatus.ENABLE.getCode() == storeTaskStatus && TaskStatus.DISABLE == currentJobStatus) {
            log.info("[quartz], configRefreshTask, resume task, job name={}", jobName);
            schedulerService.resumeJob(jobName, QuartzConstant.DEFAULT_JOB_GROUP);
        }
        if (TaskStatus.DISABLE.getCode() == storeTaskStatus && TaskStatus.ENABLE == currentJobStatus) {
            log.info("[quartz], configRefreshTask, pause task, job name={}", jobName);
            schedulerService.pauseJob(jobName, QuartzConstant.DEFAULT_JOB_GROUP);
        }
    }
}