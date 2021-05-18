package com.cg.quartz.api.service;

import com.cg.quartz.api.TaskManagerApi;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.utils.ObjectUtils;
import com.cg.quartz.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * 任务管理rmi服务端
 * <pre>
 *     启用rmi服务, 引入quartz的项目(目标Jvm)成为rmi服务端, 监听来自quartz-admin调度请求以实时作用于目标Jvm quartz调度器(Scheduler)
 *     未启用rmi服务, quartz-admin调度请求通过更改调度任务表，配合配置自动刷新任务以完成调度请求
 *     默认此配置关闭, 启用请前往spring配置文件开启(quartz.enable-rmi: true)
 * </pre>
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/12
 * @see com.cg.quartz.task.ConfigAutoRefreshTask
 */
@Slf4j
@Service
public class TaskManagerApiRmiService {

    @Autowired
    private TaskManagerApi taskManagerApi;

    public void openRmiService() {
        try {
            System.setProperty("sun.rmi.transport.tcp.maxConnectionThreads", "4");
            Environment environment = SpringContextUtils.getApplicationContext().getEnvironment();
            String strPort = environment.getProperty(QuartzConstant.RMI_PORT);
            // Registry.REGISTRY_PORT * RandomUtils.getRandom(1) + RandomUtils.getRandom(1)
            int port = ObjectUtils.isBlank(strPort) ?
                    Registry.REGISTRY_PORT : Integer.parseInt(strPort);
            String topic = environment.getProperty(QuartzConstant.RMI_TOPIC, SpringContextUtils.getApplicationContext().getApplicationName());
            LocateRegistry.createRegistry(port).bind(topic, taskManagerApi);
            log.info("[quartz], taskManager rmi service start successfully, ip={}, port={}, topic={}", InetAddress.getLocalHost().getHostAddress(), port, topic);
        } catch (Exception e) {
            log.error("[quartz], taskManager rmi service startup error, caused by ==>", e);
            throw new RuntimeException(e);
        }
    }
}