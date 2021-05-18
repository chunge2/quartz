package com.cg.quartz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cg.quartz.annotaion.ConcurrentExecution;
import com.cg.quartz.annotaion.CronExpression;
import com.cg.quartz.annotaion.EnablePersist;
import com.cg.quartz.annotaion.Task;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.constant.em.TaskStatus;
import com.cg.quartz.entity.po.JobBO;
import com.cg.quartz.entity.po.TaskPO;
import com.cg.quartz.exception.QuartzException;
import com.cg.quartz.service.SchedulerManagerService;
import com.cg.quartz.service.TaskStoreService;
import com.cg.quartz.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

/**
 * Quartz管理服务实现
 *
 * @author chunges
 * @version 1.0
 * @date 2020-08-08
 */
@Slf4j
@Service
public class SchedulerManagerServiceImpl implements SchedulerManagerService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired(required = false)
    private TaskStoreService taskStoreService;

    @Autowired
    private Environment environment;

    /**
     * 任务Task缓存<taskName, TaskPO>
     */
    private Map<String, TaskPO> cacheStoreTasks;

    /**
     * 是否启用持久化
     */
    private Boolean persistence;

    /**
     * 初始化任务配置
     */
    private void initStoreConfig() {
        // 检查配置文件是否启用持久化或使用@EnablePersist
        Optional.ofNullable(environment.getProperty(QuartzConstant.ENABLE_PERSIST_FLAG)).ifPresent(v -> persistence = Boolean.valueOf(v));
        persistence = Optional.ofNullable(persistence).orElseGet(() -> ObjectUtils.notEmpty(SpringContextUtils.getBeansWithAnnotation(EnablePersist.class)));
        log.info("[quartz] initStoreConfig, enablePersist status={}", persistence);

        // 初始化任务
        List<TaskPO> storeTasks = persistence ? taskStoreService.list(new QueryWrapper<>(TaskPO.builder().status(TaskStatus.ENABLE.getCode()).build())) : new ArrayList<>();
        boolean notEmptyTasks = ObjectUtils.notEmpty(storeTasks);
        cacheStoreTasks = notEmptyTasks ? new HashMap<>(storeTasks.size()) : new HashMap<>(0);
        for (TaskPO storeTask : storeTasks) {
            cacheStoreTasks.put(storeTask.getTaskName(), storeTask);
        }

        // 持久化模式数据库未配置自动刷新配置任务则手动添加

        if (persistence && ObjectUtils.isNull(cacheStoreTasks.get(QuartzConstant.CONFIG_AUTO_REFRESH_TASK))) {
            cacheStoreTasks.put(QuartzConstant.CONFIG_AUTO_REFRESH_TASK,
                    new TaskPO(QuartzConstant.CONFIG_AUTO_REFRESH_TASK, QuartzConstant.DEFAULT_CRON, QuartzConstant.EMPTY_STRING));
            log.warn("[quartz], configRefreshTask not found from database, use default setting, taskName={}, cron={}",
                    QuartzConstant.CONFIG_AUTO_REFRESH_TASK, QuartzConstant.DEFAULT_CRON);
        }
        if (persistence && !notEmptyTasks) {
            log.warn("[quartz], the task config from database is empty!");
        }
    }

    @Override
    public void registerJobs() {
        // runTask标签(命令行参数优先级更高)未配置或为false不加载任务
        String canRunTask = Optional.ofNullable(System.getProperty(QuartzConstant.RUN_TASK_COMMAND_LINE_FLAG))
                .orElseGet(() -> environment.getProperty(QuartzConstant.RUN_TASK_CONFIG_FLAG));
        if (Boolean.valueOf(canRunTask)) {
            runTask();
        }
    }

    @Override
    public JobBO getJob(String jobName, String jobGroup) {
        try {
            Assert.isTrue(ObjectUtils.notNull(jobName) && ObjectUtils.notNull(jobGroup), "job name or job group is null");
            JobKey jobKey = new JobKey(jobName, jobGroup);
            if (ObjectUtils.notNull(scheduler.getJobDetail(jobKey))) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                return getJobInformation(jobKey, triggers.get(QuartzConstant.ZERO));
            }
            return null;
        } catch (SchedulerException e) {
            log.error("[quartz], get job catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public List<JobBO> listJobs() {
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
            List<JobBO> jobBos = new ArrayList<>(jobKeys.size());
            for (JobKey jobKey : jobKeys) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                for (Trigger trigger : triggers) {
                    if (!(trigger instanceof CronTrigger)) {
                        continue;
                    }
                    jobBos.add(getJobInformation(jobKey, trigger));
                }
            }
            return jobBos;
        } catch (SchedulerException e) {
            log.error("[quartz], list job messages catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public void runJobNow(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            if (ObjectUtils.notNull(scheduler.getJobDetail(jobKey))) {
                scheduler.triggerJob(jobKey);
            }
        } catch (Exception e) {
            log.error("[quartz], run job now catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public void addJob(String jobName, Object jobInstance) {
        try {
            Assert.isTrue(ObjectUtils.notBlank(jobName) && ObjectUtils.notNull(jobInstance), "job name or job instance is null," + jobName);
            boolean isImplementJob = jobInstance instanceof Job;
            Assert.isTrue(isImplementJob || jobInstance.getClass().isAnnotationPresent(Task.class),
                    "can't accept job instance type, jobName is " + jobName);

            // 实现Job接口添加任务
            if (isImplementJob) {
                Map<String, Job> jobContainer = SpringContextUtils.getBeansOfType(Job.class);
                Job job = jobContainer.get(jobName);
                Assert.notNull(job, "job can't found in spring container, job =" + jobName);
                activeJob(jobName, job.getClass());
                return;
            }

            // @Task注解添加任务
            Map<String, Object> taskContainer = SpringContextUtils.getBeansWithAnnotation(Task.class);
            Object task = taskContainer.get(jobName);
            Assert.notNull(task, "job can't found in spring container, job =" + jobName);
            TaskPO tempTaskPo = TaskPO.builder().taskName(jobName).build();
            TaskPO taskPo = persistence ? Optional.ofNullable(taskStoreService.getOne(new QueryWrapper<>(tempTaskPo)))
                    .orElseThrow(() -> new QuartzException("get task from DB is null, task :" + jobName)) : tempTaskPo;
            Optional.ofNullable(buildJobDetailFactoryBean(taskPo, task)).ifPresent(ExceptionUtils.handleCheckedException(this::activeTask));
        } catch (Exception e) {
            log.error("[quartz], add job catch a exception, caused by ==>{}", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public void pauseJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            if (ObjectUtils.notNull(scheduler.getJobDetail(jobKey))) {
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("[quartz], pause job catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public void pauseAll() {
        ExceptionUtils.handleCheckedException(r -> scheduler.pauseAll()).accept(null);
    }

    @Override
    public void resumeJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            if (ObjectUtils.notNull(scheduler.getJobDetail(jobKey))) {
                scheduler.resumeJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("[quartz], resume job catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public void resumeAll() {
        ExceptionUtils.handleCheckedException(e -> scheduler.resumeAll()).accept(null);
    }

    @Override
    public boolean modifyJob(String triggerName, String triggerGroup, String cronExpression) {
        try {
            Date date = null;
            TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroup);
            CronTrigger cronTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (ObjectUtils.isNull(cronTrigger)) {
                log.info("triggerName={} not exists in scheduler", triggerName);
                return false;
            }

            String oldCronExpression = cronTrigger.getCronExpression();
            if (!oldCronExpression.equalsIgnoreCase(cronExpression)) {
                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
                CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, triggerGroup)
                        .withSchedule(cronScheduleBuilder).build();
                date = scheduler.rescheduleJob(triggerKey, trigger);
            }
            return date != null;
        } catch (SchedulerException e) {
            log.error("[quartz], modify job catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public boolean deleteJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            if (ObjectUtils.isNull(scheduler.getJobDetail(jobKey))) {
                return false;
            }
            return scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("[quartz], delete job catch a exception, caused by ==>", e.getMessage());
            throw new QuartzException(e);
        }
    }

    @Override
    public boolean isLoaded(String jobName, String jobGroup) {
        try {
            return ObjectUtils.notNull(scheduler.getJobDetail(JobKey.jobKey(jobName, jobGroup)));
        } catch (Exception e) {
            throw new QuartzException(e);
        }
    }

    @Override
    public boolean getPersistenceStatus() {
        return persistence;
    }

    private void runTask() {
        try {
            initStoreConfig();
            loadTasks();
            loadJobs();
            scheduler.start();
            // gc
            cacheStoreTasks = null;
        } catch (Exception e) {
            log.error("[quartz] register jobs catch a exception, caused by ==>", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 装载@Task注解任务
     * <pre>
     *     持久化模式从数据库加载Task配置; 内存模式加载所有持有Task注解任务(带有Task注解同时实现job接口,Job接口优先)
     * </pre>
     */
    private void loadTasks() throws Exception {
        List<MethodInvokingJobDetailFactoryBean> jobDetailsBeans = buildJobDetailFactoryBeans();
        for (MethodInvokingJobDetailFactoryBean jobDetailBean : jobDetailsBeans) {
            if (ObjectUtils.isNull(jobDetailBean) || ObjectUtils.isNull(jobDetailBean.getObject())) {
                log.info("[quartz], jobDetailBean or jobDetail is null");
                continue;
            }
            activeTask(jobDetailBean);
        }
    }

    /**
     * 装载Job接口任务
     *
     * <pre>
     *     持久化模式从数据库加载Job配置; 内存模式加载所有实现Job接口任务
     * </pre>
     */
    private void loadJobs() throws SchedulerException {
        Map<String, Job> jobContainer = SpringContextUtils.getBeansOfType(Job.class);
        if (persistence) {
            TaskPO task;
            for (Map.Entry<String, TaskPO> taskEntry : cacheStoreTasks.entrySet()) {
                task = taskEntry.getValue();
                // 数据库中的Job必须已被Spring加载
                if (ObjectUtils.isNull(jobContainer.get(task.getTaskName()))) {
                    continue;
                }
                activeJob(task.getTaskName(), jobContainer.get(task.getTaskName()).getClass());
            }
            return;
        }

        for (Map.Entry<String, Job> jobEntry : jobContainer.entrySet()) {
            activeJob(jobEntry.getKey(), jobEntry.getValue().getClass());
        }
    }

    /**
     * 构建所有@Task bean
     *
     * @return MethodInvokingJobDetailFactoryBean List
     */
    private List<MethodInvokingJobDetailFactoryBean> buildJobDetailFactoryBeans() throws Exception {
        Map<String, Object> taskContainer = SpringContextUtils.getBeansWithAnnotation(Task.class);
        List<MethodInvokingJobDetailFactoryBean> beansList = new ArrayList<>(persistence ? cacheStoreTasks.size() : taskContainer.size());
        // DB model
        if (persistence) {
            TaskPO task;
            for (Map.Entry<String, TaskPO> taskEntry : cacheStoreTasks.entrySet()) {
                task = taskEntry.getValue();
                // 数据库中的Task必须已被Spring加载
                if (ObjectUtils.isNull(taskContainer.get(task.getTaskName()))) {
                    continue;
                }
                beansList.add(buildJobDetailFactoryBean(task, taskContainer.get(task.getTaskName())));
            }
            return beansList;
        }

        // RAM model
        TaskPO taskPo = new TaskPO();
        for (Map.Entry<String, Object> taskEntry : taskContainer.entrySet()) {
            // RAM模式下跳过自动刷新配置
            if (QuartzConstant.CONFIG_AUTO_REFRESH_TASK.equals(taskEntry.getKey())) {
                continue;
            }
            taskPo.setTaskName(taskEntry.getKey());
            beansList.add(buildJobDetailFactoryBean(taskPo, taskContainer.get(taskEntry.getKey())));
        }
        return beansList;
    }

    /**
     * 构建任务FactoryBean
     *
     * @param taskPo     任务PO(用于存储执行方法, 是否并发执行)
     * @param memoryTask 内存task(spring容器中Task, 用于非持久化模式下获取@Task相关信息)
     * @return JobDetailFactoryBean
     * @throws Exception exception
     */
    private MethodInvokingJobDetailFactoryBean buildJobDetailFactoryBean(TaskPO taskPo, Object memoryTask) throws Exception {
        if (memoryTask instanceof Job) {
            log.warn("[quartz], this class with @Task annotation and implement Job interface! use Job interface, taskName={}", taskPo.getTaskName());
            return null;
        }
        String targetMethod = null;
        String taskDescription = null;
        boolean allowConcurrent = false;
        if (!persistence) {
            // Task执行方法获取: 持久化模式: DB配置; RAM模式: @Task#method()
            Task taskAnnotation = memoryTask.getClass().getAnnotation(Task.class);
            allowConcurrent = taskAnnotation.allowConcurrent();
            targetMethod = Optional.of(taskAnnotation.method()).orElse(QuartzConstant.EXECUTE_METHOD);
            taskDescription = Optional.of(taskAnnotation.description()).orElse(QuartzConstant.EMPTY_STRING);
        }
        targetMethod = persistence ? taskPo.getMethod() : targetMethod;
        Assert.notBlank(targetMethod, "terminate load task, target method is null, taskName=" + taskPo.getTaskName());

        taskDescription = persistence ? taskPo.getDescription() : taskDescription;
        allowConcurrent = persistence ? TaskStatus.ENABLE.getCode() == taskPo.getAllowConcurrent() : allowConcurrent;
        MethodInvokingJobDetailFactoryBean jobDetailFactoryBean = QuartzTaskUtils.buildJobDetailFactoryBean(taskPo.getTaskName(), targetMethod, allowConcurrent);
        jobDetailFactoryBean.setBeanFactory(beanFactory);

        // @Task任务类需运行的JobDetail未初始化, 手动调用初始化(必须手动调用且需设置beanFactory)
        jobDetailFactoryBean.afterPropertiesSet();

        // 设置任务描述信息(JobDetail默认实现是JobDetailImpl)
        if (jobDetailFactoryBean.getObject() instanceof JobDetailImpl) {
            ((JobDetailImpl) jobDetailFactoryBean.getObject()).setDescription(taskDescription);
        }
        return jobDetailFactoryBean;
    }

    /**
     * 激活Task
     *
     * @param jobDetailBean jobDetailBean
     * @throws ParseException     ParseException
     * @throws SchedulerException SchedulerException
     */
    private void activeTask(MethodInvokingJobDetailFactoryBean jobDetailBean) throws ParseException, SchedulerException {
        JobDetail jobDetail = jobDetailBean.getObject();
        String taskName = jobDetail.getKey().getName();
        String cron = getCron(taskName, SpringContextUtils.getBean(taskName));
        String logFormat = persistence ? "[quartz] load task(DB) success, name={}, cron={}, targetMethod={}, desc={}"
                : "[quartz] load task(RAM) success, name={}, cron={}, targetMethod={}, desc={}";
        log.info(logFormat, taskName, cron, jobDetailBean.getTargetMethod(), jobDetail.getDescription());
        CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
        cronTriggerFactoryBean.setName(taskName);
        cronTriggerFactoryBean.setGroup(QuartzConstant.DEFAULT_CRON_TRIGGER_GROUP);
        cronTriggerFactoryBean.setCronExpression(cron);
        cronTriggerFactoryBean.setJobDetail(jobDetail);
        cronTriggerFactoryBean.afterPropertiesSet();
        scheduler.scheduleJob(jobDetail, cronTriggerFactoryBean.getObject());
    }

    /**
     * 激活Job
     *
     * @param jobName  jobName/ job key name/ trigger key name
     * @param jobClass job class(必须传入该值, 使用Job.class则关闭并行执行将不会生效)
     * @throws SchedulerException schedulerException
     */
    private void activeJob(String jobName, Class<? extends Job> jobClass) throws SchedulerException {
        JobBO jobBo = getJobBuildInformation(jobName, jobClass);

        //  StatefulMethodInvokingJob不并发作业, 默认Job是并发
        Class<? extends Job> jobClazz = jobBo.getAllowConcurrent() ? jobClass : MethodInvokingJobDetailFactoryBean.StatefulMethodInvokingJob.class;
        JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity(jobName, QuartzConstant.DEFAULT_JOB_GROUP)
                .withDescription(jobBo.getDescription()).ofType(jobClazz).build();
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, QuartzConstant.DEFAULT_CRON_TRIGGER_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(jobBo.getCronExpression())).build();
        String logFormat = persistence ? "[quartz] load job(DB) success, name={}, cron={}, allowConcurrent={}, desc={}"
                : "[quartz] load job(RAM) success, name={}, cron={}, allowConcurrent={}, desc={}";
        log.info(logFormat, jobName, jobBo.getCronExpression(), jobBo.getAllowConcurrent(), jobBo.getDescription());
        scheduler.scheduleJob(jobDetail, cronTrigger);
    }

    /**
     * 获取Job构建信息(初始化加载和添加使用)
     *
     * @param jobName  任务名
     * @param jobClass job class
     * @return Job构建信息
     */
    private JobBO getJobBuildInformation(String jobName, Class<? extends Job> jobClass) {
        if (persistence) {
            // 初始化加载Job从缓存Task构建, 后续添加Job实时查询
            TaskPO taskPo = ObjectUtils.notEmpty(cacheStoreTasks) ?
                    cacheStoreTasks.get(jobName) : taskStoreService.getOne(new QueryWrapper<>(TaskPO.builder().taskName(jobName).build()));
            Assert.isTrue(ObjectUtils.notNull(taskPo) && ObjectUtils.notBlank(taskPo.getCronExpression()),
                    "terminate load task, can't get cron expression(persistence), beanName=" + jobName);
            return JobBO.builder()
                    .jobName(jobName).cronExpression(taskPo.getCronExpression())
                    .allowConcurrent(TaskStatus.ENABLE.getCode() == taskPo.getAllowConcurrent())
                    .description(taskPo.getDescription())
                    .build();
        }
        String cron = jobClass.isAnnotationPresent(CronExpression.class) ? jobClass.getAnnotation(CronExpression.class).value() : null;
        Assert.notBlank(cron, "terminate load task, can't get cron expression(RAM), beanName=" + jobName);
        boolean allowConcurrentExecution = jobClass.isAnnotationPresent(ConcurrentExecution.class) && jobClass.getAnnotation(ConcurrentExecution.class).value();
        return JobBO.builder()
                .jobName(jobName).cronExpression(cron)
                .allowConcurrent(allowConcurrentExecution)
                .description(QuartzConstant.EMPTY_STRING)
                .build();
    }

    /**
     * 获取当前Job信息
     * <p>获取指定JobKey和Trigger包含job信息</p>
     *
     * @param jobKey  任务名
     * @param trigger 触发器
     * @return Job信息
     * @throws SchedulerException schedulerException
     */
    private JobBO getJobInformation(JobKey jobKey, Trigger trigger) throws SchedulerException {
        TriggerKey triggerKey = trigger.getKey();
        boolean enable = Trigger.TriggerState.NORMAL == scheduler.getTriggerState(triggerKey) ||
                Trigger.TriggerState.BLOCKED == scheduler.getTriggerState(triggerKey);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        // 是StatefulMethodInvokingJob同类或父接口表示不允许并发执行
        boolean disallowConcurrent = MethodInvokingJobDetailFactoryBean.StatefulMethodInvokingJob.class.isAssignableFrom(jobDetail.getJobClass());
        return JobBO.builder()
                .jobName(jobKey.getName()).jobGroup(jobKey.getGroup())
                .trigger(triggerKey.getName()).triggerGroup(triggerKey.getGroup())
                .cronExpression(((CronTrigger) trigger).getCronExpression())
                .triggerState(scheduler.getTriggerState(triggerKey)).status(enable ? TaskStatus.ENABLE : TaskStatus.DISABLE)
                .description(jobDetail.getDescription())
                .allowConcurrent(!disallowConcurrent)
                .build();
    }

    /**
     * 配置cron表达式  持久化模式: 数据库CronExpression; RAM模式: @CronExpression > @Task.cron()
     *
     * @param taskName    任务名
     * @param jobInstance 任务实例
     * @return cron表达式
     */
    private String getCron(String taskName, Object jobInstance) {
        String cron;
        if (persistence) {
            cron = ObjectUtils.notEmpty(cacheStoreTasks) ? cacheStoreTasks.get(taskName).getCronExpression()
                    : Optional.ofNullable(taskStoreService.getOne(new QueryWrapper<>(TaskPO.builder().taskName(taskName).build())))
                    .orElseThrow(() -> new RuntimeException("task get from database is empty, taskName=" + taskName)).getCronExpression();
            Assert.notBlank(cron, "can't get a cron expression(persistence), taskName=" + taskName);
            return cron;
        }
        cron = jobInstance.getClass().isAnnotationPresent(CronExpression.class) ? jobInstance.getClass().getAnnotation(CronExpression.class).value() : null;
        cron = ObjectUtils.isBlank(cron) ? jobInstance.getClass().getAnnotation(Task.class).cron() : cron;
        Assert.notBlank(cron, "can't get a cron expression(RAM), taskName=" + taskName);
        return cron;
    }

}