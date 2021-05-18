package com.cg.quartz.service;

import java.util.Date;
import java.util.Map;

/**
 * 任务分析服务
 *
 * @author chunge
 * @version 1.0
 * @date 2021/1/16
 */
public interface TaskAnalysisService {

    /**
     * 获取任务总数
     *
     * @return 任务总数
     */
    int countTask();

    /**
     * 按任务状态(开启和关闭)分组统计
     *
     * @return 任务状态的任务数量
     */
    Map<Boolean, Integer> countTaskByStatus();

    /**
     * 按任务执行结果状态分组统计
     * <pre>
     *     任务自身不带执行结果, 被分析任务必须使用@TaskLog输出日志以通过分析日志表信息获取任务执行结果
     *     开始时间和结束时间未null将统计所有次数
     * </pre>
     *
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @return 任务按状态分组的调用次数
     */
    Map<Boolean, Integer> countTaskInvokeTimeBySatus(Date beginDate, Date endDate);

    // 某日成功日志List 某日失败日志List
    // 统计每天异常次数(按异常头统计), 异常百分比(前端完成), 当前时间异常信息输出, 普通信息输出, 异常信息平均分布, 日志量平均分布
}