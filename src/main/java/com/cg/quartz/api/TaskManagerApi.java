package com.cg.quartz.api;

import com.cg.quartz.api.req.JobReq;
import com.cg.quartz.api.resp.JobResp;
import com.cg.quartz.api.result.RpcResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Quartz管理API
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/17
 */
public interface TaskManagerApi extends Remote {

    /**
     * 立即触发Job
     * <pre>
     *   如果该任务尚未被调度器加载过, 调用此接口后会自动加载(若启用持久化也会同步更新状态)
     * </pre>
     *
     * @param task request params(job name, job group必传)
     * @return 调度结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> runJobNow(JobReq task) throws RemoteException;

    /**
     * 修改任务信息
     * <pre>
     *     可修改正在运行(包括暂停状态)或未运行任务信息
     *     正在运行: 修改任务相关信息及调度器中表达式(如果cron传入)
     *     未运行: 仅修改任务相关信息
     *     注意:
     *     1 修改任务相关信息(表达式, 执行方法, 并发执行状态, 任务描述)仅支持持久化模式
     *     2 任务状态不在此接口修改, 并发状态和任务执行方法调度器未加载可生效，已加载则修改后需重启应用生效
     * </pre>
     *
     * @param job request params(trigger, trigger group必传)
     * @return 修改结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> modifyJob(JobReq job) throws RemoteException;

    /**
     * 暂停任务
     *
     * @param job request params(job name, job group必传)
     * @return 暂停结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> pauseJob(JobReq job) throws RemoteException;

    /**
     * 暂停所有任务
     *
     * @return 暂停结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> pauseAll() throws RemoteException;

    /**
     * 恢复任务
     *
     * @param job request params(job name, job group必传)
     * @return 恢复结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> resumeJob(JobReq job) throws RemoteException;

    /**
     * 恢复所有任务
     *
     * @return 恢复结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> resumeAll() throws RemoteException;

    /**
     * 删除某个任务(job name, job group必传)
     *
     * @param job delete job request params
     * @return 删除结果(true: 成功, false: 失败)
     * @throws RemoteException RemoteException
     */
    RpcResult<Boolean> deleteJob(JobReq job) throws RemoteException;

    /**
     * 获取Job信息
     *
     * @param job request params(job name, job group必传)
     * @return job信息
     * @throws RemoteException RemoteException
     */
    RpcResult<JobResp> getJobMessage(JobReq job) throws RemoteException;

    /**
     * 获取所有Job信息
     *
     * @return Job信息列表
     * @throws RemoteException RemoteException
     */
    RpcResult<List<JobResp>> listJobMessages() throws RemoteException;
}