package com.cg.quartz.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cg.quartz.annotaion.Task;
import com.cg.quartz.api.TaskManagerApi;
import com.cg.quartz.api.req.JobReq;
import com.cg.quartz.api.resp.JobResp;
import com.cg.quartz.api.result.RpcResult;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.constant.em.TaskStatus;
import com.cg.quartz.entity.po.JobBO;
import com.cg.quartz.entity.po.TaskPO;
import com.cg.quartz.service.SchedulerManagerService;
import com.cg.quartz.service.TaskStoreService;
import com.cg.quartz.utils.Assert;
import com.cg.quartz.utils.ObjectUtils;
import com.cg.quartz.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Quartz管理API实现
 *
 * @author chunge
 * @version 1.0
 * @date 2020/10/22
 */
@Slf4j
@Service
public class TaskManagerApiImpl extends UnicastRemoteObject implements TaskManagerApi {

    @Autowired
    private SchedulerManagerService schedulerManager;

    @Autowired(required = false)
    private TaskStoreService taskStoreService;

    protected TaskManagerApiImpl() throws RemoteException {
    }

    @Override
    public RpcResult<Boolean> runJobNow(JobReq job) {
        try {
            logInAndBaseVerify(job, "runJobNow");
            String jobName = job.getJobName();

            // job尚未被调度器加载尝试从容器中加载
            if (!schedulerManager.isLoaded(jobName, job.getJobGroup())) {
                Object jobInstance = Optional.ofNullable(SpringContextUtils.getBeansWithAnnotation(Task.class).get(jobName))
                        .orElseGet(() -> SpringContextUtils.getBeansOfType(Job.class).get(jobName));
                Assert.notNull(jobInstance, "the task to be run is empty");
                schedulerManager.addJob(jobName, jobInstance);
                if (schedulerManager.getPersistenceStatus()) {
                    // updateValue, updateCondition
                    taskStoreService.update(TaskPO.builder().status(TaskStatus.ENABLE.getCode()).build(),
                            new UpdateWrapper<>(TaskPO.builder().taskName(jobName).status(TaskStatus.DISABLE.getCode()).build()));
                }
            }
            schedulerManager.runJobNow(jobName, job.getJobGroup());
            log.info("[quartz], api response, runJobNow successfully, name={}, group={}", job.getJobName(), job.getJobGroup());
            return RpcResult.buildSuccessResp(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, run job now catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<Boolean> modifyJob(JobReq job) {
        try {
            Assert.isTrue(ObjectUtils.notNull(job), "job request params is null");
            log.info("[quartz], api request, modifyJob, trigger={}, trigger group={}, cron={}, method={}, desc={}, allowConcurrent={}, persistence={}",
                    job.getTrigger(), job.getTriggerGroup(), job.getCronExpression(), job.getMethod(), job.getDescription(), job.getAllowConcurrent(), schedulerManager.getPersistenceStatus());
            Assert.isTrue(ObjectUtils.notBlank(job.getTrigger()) && ObjectUtils.notBlank(job.getTriggerGroup()), "trigger name or group is null");
            boolean updateDataBaseResult = false;

            // 持久化模式可更新task相关配置(method,allowConcurrent设置后需重启生效), 但不处理任务状态
            if (schedulerManager.getPersistenceStatus()) {
                TaskPO updateTask = new TaskPO();
                BeanUtils.copyProperties(job, updateTask);
                Optional.ofNullable(job.getAllowConcurrent()).ifPresent(r -> updateTask.setAllowConcurrent(job.getAllowConcurrent() ? QuartzConstant.ONE : QuartzConstant.ZERO));
                updateDataBaseResult = taskStoreService.update(updateTask, new UpdateWrapper<>(TaskPO.builder().taskName(job.getTrigger()).build()));
            }

            // 若cron表达式为空无需更新调度器中Job
            if (ObjectUtils.isBlank(job.getCronExpression())) {
                log.info("[quartz], api response, because the cron of {} is null, modify job ignored", job.getJobName());
                return RpcResult.buildSuccessResp(Boolean.TRUE, "cron is null, ignore update scheduler operation");
            }
            boolean updateSchedulerResult = schedulerManager.modifyJob(job.getTrigger(), job.getTriggerGroup(), job.getCronExpression());
            log.info("[quartz], modify job api response, trigger={}, result={}", job.getTrigger(), updateDataBaseResult, updateSchedulerResult);
            return updateSchedulerResult || updateDataBaseResult ? RpcResult.buildSuccessResp(Boolean.TRUE) : RpcResult.buildFailResp(Boolean.FALSE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, modify job catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<Boolean> pauseJob(JobReq job) {
        log.info("log={}, readTimeout={}, maxConnectionThreads={}, threadKeepAliveTime={}",   System.getProperty("java.rmi.server.logCalls")
        , System.getProperty("sun.rmi.transport.tcp.readTimeout"),  System.getProperty("sun.rmi.transport.tcp.maxConnectionThreads")
        , System.getProperty("sun.rmi.transport.tcp.threadKeepAliveTime"));
        try {
            logInAndBaseVerify(job, "pauseJob");
            if (!schedulerManager.isLoaded(job.getJobName(), job.getJobGroup())) {
                log.info("[quartz], pause job response, task {} is not loaded by the scheduler and update failed", job.getJobName());
                return RpcResult.buildFailResp(Boolean.FALSE, job.getJobName() + " is not loaded by the scheduler, ignore this operation");
            }

            // 持久化模式先更新数据库任务再更新调度器任务(自动刷新配置任务总是将调度器中任务和数据库同步)
            if (schedulerManager.getPersistenceStatus()) {
                taskStoreService.update(TaskPO.builder().status(TaskStatus.DISABLE.getCode()).build(),
                        new UpdateWrapper<>(TaskPO.builder().taskName(job.getJobName()).status(TaskStatus.ENABLE.getCode()).build()));
            }
            schedulerManager.pauseJob(job.getJobName(), job.getJobGroup());
            log.info("[quartz], api response, pauseJob successfully, job name={}, job group={}", job.getJobName(), job.getJobGroup());
            return RpcResult.buildSuccessResp(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, pause job catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<Boolean> pauseAll() {
        try {
            log.info("[quartz], api request, pause all jobs, persistence={}", schedulerManager.getPersistenceStatus());
            schedulerManager.pauseAll();
            if (schedulerManager.getPersistenceStatus()) {
                taskStoreService.update(TaskPO.builder().status(TaskStatus.DISABLE.getCode()).build(),
                        new UpdateWrapper<>(TaskPO.builder().status(TaskStatus.ENABLE.getCode()).build()));
            }
            log.info("[quartz], api response, pause all jobs successfully");
            return RpcResult.buildSuccessResp(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, pause all jobs catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<Boolean> resumeJob(JobReq job) {
        try {
            // 恢复任务可以是未被调度器加载的Task
            logInAndBaseVerify(job, "resumeJob");
            boolean updateResult = false;
            if (schedulerManager.getPersistenceStatus()) {
                updateResult = taskStoreService.update(TaskPO.builder().status(TaskStatus.ENABLE.getCode()).build(),
                        new UpdateWrapper<>(TaskPO.builder().taskName(job.getJobName()).status(TaskStatus.DISABLE.getCode()).build()));
            }

            if (!schedulerManager.isLoaded(job.getJobName(), job.getJobGroup()) && !updateResult) {
                log.info("[quartz], resume job response, task {} is not loaded by the scheduler and update failed", job.getJobName());
                return RpcResult.buildFailResp(Boolean.FALSE, job.getJobName() + " is not loaded by the scheduler, ignore this operation");
            }
            schedulerManager.resumeJob(job.getJobName(), job.getJobGroup());
            log.info("[quartz], api response, resume job successfully, job name={}, job group={}", job.getJobName(), job.getJobGroup());
            return RpcResult.buildSuccessResp(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, resume job catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<Boolean> resumeAll() {
        try {
            log.info("[quartz], api request, resume all jobs, persistence={}", schedulerManager.getPersistenceStatus());
            if (schedulerManager.getPersistenceStatus()) {
                taskStoreService.update(TaskPO.builder().status(TaskStatus.ENABLE.getCode()).build(),
                        new UpdateWrapper<>(TaskPO.builder().status(TaskStatus.DISABLE.getCode()).build()));
            }
            schedulerManager.resumeAll();
            log.info("[quartz], api response, resume all jobs successfully");
            return RpcResult.buildSuccessResp(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, resume all jobs catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<Boolean> deleteJob(JobReq job) {
        try {
            logInAndBaseVerify(job, "deleteJob");
            if (schedulerManager.getPersistenceStatus()) {
                taskStoreService.remove(new QueryWrapper<>(TaskPO.builder().taskName(job.getJobName()).build()));
            }
            boolean deleteResult = schedulerManager.deleteJob(job.getJobName(), job.getJobGroup());
            log.info("[quartz], delete job api response, job name={}, deleteResult={}", job.getJobName(), deleteResult);
            return RpcResult.buildSuccessResp(Boolean.TRUE);
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, delete job catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<JobResp> getJobMessage(JobReq job) {
        try {
            logInAndBaseVerify(job, "getJobMessage");
            JobResp jobResp = new JobResp();
            TaskPO taskPo = schedulerManager.getPersistenceStatus() ?
                    taskStoreService.getOne(new QueryWrapper<>(TaskPO.builder().taskName(job.getJobName()).build())) : null;
            copyProperties(taskPo, jobResp);

            // jobResp = taskPo + jobBo, 可能存在taskPo或jobBo为空
            JobBO jobBo = schedulerManager.getJob(job.getJobName(), job.getJobGroup());
            Optional.ofNullable(jobBo).ifPresent(r -> BeanUtils.copyProperties(jobBo, jobResp));
            log.info("[quartz], api response, getJobMessage, jobResp={}", jobResp);
            return ObjectUtils.notNull(jobResp.getJobName()) ? RpcResult.buildSuccessResp(jobResp) : RpcResult.buildSuccessResp("empty result set");
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, get a job message catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e);
        }
    }

    @Override
    public RpcResult<List<JobResp>> listJobMessages() {
        try {
            log.info("[quartz], api request, list all jobs");
            List<JobBO> jobBos = schedulerManager.listJobs();
            List<JobResp> jobRespList = ObjectUtils.notEmpty(jobBos) ? new ArrayList<>(jobBos.size()) : null;
            Map<String, JobResp> jobRespMap = ObjectUtils.notEmpty(jobBos) ? new HashMap<>(jobBos.size()) : new HashMap<>(16);
            if (schedulerManager.getPersistenceStatus()) {
                JobResp jobResp;
                List<TaskPO> taskPos = taskStoreService.list();
                if (ObjectUtils.isEmpty(taskPos) && ObjectUtils.isEmpty(jobBos)) {
                    return RpcResult.buildFailResp(null, "all jobs does not exist");
                }
                jobRespList = ObjectUtils.isEmpty(jobRespList) ? new ArrayList<>(taskPos.size()) : jobRespList;
                for (TaskPO taskPo : taskPos) {
                    copyProperties(taskPo, jobResp = new JobResp());
                    jobRespMap.put(taskPo.getTaskName(), jobResp);
                    jobRespList.add(jobResp);
                }
            }
            // jobRespList = taskPos + jobBos
            boolean isEmptyStoreTask = ObjectUtils.isEmpty(jobRespMap);
            for (JobBO jobBo : jobBos) {
                JobResp jobResp = jobRespMap.getOrDefault(jobBo.getJobName(), new JobResp());
                BeanUtils.copyProperties(jobBo, jobResp);
                if (isEmptyStoreTask) {
                    jobRespList.add(jobResp);
                }
            }
            log.info("[quartz], api response, list all jobs, size={}", ObjectUtils.notEmpty(jobRespList) ? jobRespList.size() : null);
            return ObjectUtils.notEmpty(jobRespList) ? RpcResult.buildSuccessResp(jobRespList) : RpcResult.buildSuccessResp("empty result set");
        } catch (Exception e) {
            log.error("[quartz], api invoke failed, list all jobs catch a exception, caused by ==>", e);
            return RpcResult.buildFailResp(null, e.getMessage());
        }
    }

    /**
     * 入参日志打印和基本参数检查
     *
     * @param job      request job params(验证job名和job分组)
     * @param logTopic log topic
     */
    private void logInAndBaseVerify(JobReq job, String logTopic) {
        Assert.notNull(job, "job request params is null");
        log.info("[quartz], api request, " + logTopic + ", job name={}, job group={}, persistence={}",
                job.getJobName(), job.getJobGroup(), schedulerManager.getPersistenceStatus());
        Assert.isTrue(ObjectUtils.notBlank(job.getJobName()) && ObjectUtils.notBlank(job.getJobGroup()), "job name or job group is null");
    }

    /**
     * 复制属性
     * <pre>
     *     1 调度器中部分字段(执行方法, 创建时间, 更新时间)可能无法获取
     *     2 数据库任务未加载到调度器, 调度器缺少该部分信息
     * </pre>
     *
     * @param source 源TaskPO
     * @param target 目标Job Response
     */
    private void copyProperties(TaskPO source, JobResp target) {
        if (ObjectUtils.notNull(source) && ObjectUtils.notNull(target)) {
            // 在只有数据库任务时Trigger可能未被赋值
            target.setJobName(source.getTaskName());
            target.setTrigger(source.getTaskName());
            target.setStatus(TaskStatus.getTaskStatus(source.getStatus()));
            target.setAllowConcurrent(TaskStatus.ENABLE.getCode() == source.getAllowConcurrent());

            target.setMethod(source.getMethod());
            target.setDescription(source.getDescription());
            target.setCronExpression(source.getCronExpression());
            target.setCreateTime(source.getCreateTime());
            target.setUpdateTime(source.getUpdateTime());
        }
    }
}