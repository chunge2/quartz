# quartz
基于Quartz轻量级封装定时任务</p>
<pre>
1.特性:
    1.1  spring,mybatis无缝衔接
    1.2. 无配置文件
    1.3  注解无侵入使用(同时支持接口和原生的启动方式)
    1.4  定时任务日志支持
    1.5  RMI接口实时控制
1.2 使用:
   仅需三步即可快速启用
   0. 安装jar到本地仓库(或导入IDEA安装)
     下载源码后解压用maven安装到本地仓库
     cd C:\Users\Admin\Downloads\quartz-main
     mvn clean & mvn install
     -- 安装jar到本地仓库
     mvn install:install-file -Dfile=C:\Users\Admin\Downloads\quartz-main\target\quartz-1.0-SNAPSHOT.jar -DgroupId=com.cg.quartz -DartifactId=quartz -Dversion=1.0-SNAPSHOT -Dpackaging=jar
   0. 启用模式
         quartz支持内存和持久化模式
         2.1: 定时任务不支持持久化配置, 每次启动配置失效
         2.2: 持久化模式需要连接数据库, 并配置相关表结构
   1. 导包</pre>
  
        <!-- quartz -->
        <dependency>
            <groupId>com.cg.quartz</groupId>
            <artifactId>quartz</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
        <!-- mybatis-plus -->
        <dependency>
           <groupId>com.baomidou</groupId>
           <artifactId>mybatis-plus-boot-starter</artifactId>
           <version>3.4.0</version>
        </dependency>
        
        <!-- springboot -->
         <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
         </dependency>
         
         <!-- spring-tx -->
          <dependency>
             <groupId>org.springframework</groupId>
             <artifactId>spring-tx</artifactId>
             <version>5.2.8.RELEASE</version>
          </dependency>
         PS: 
         1. quartz和mybatis-plus必须导入
         2. 已有spring/springBoot环境可不导入springboot包(已有mybatis-plus可不导mybatis-plus)
         3. spring-tx非持久化(内存模式)可不导入
   <pre>2. 添加定时任务
      2.1 基于@Task注解(推荐)
      新建任意类, 如RunTask.java, 新建run方法, 类上添加@Task注解
      -- 示例
      import com.cg.quartz.annotaion.Task;
      @Task(cron = "0/3 * * * * ?")
      public class RunTask {
          public void run(){
              System.out.println("Run Task....");
          }
      }
      PS: 
      1 默认执行的为run方法(@Task中可更改), 必须是公有,空参数, 空返回值
      2 cron表达式在内存模式下必须给出
      3 @Task(cron = "0/3 * * * * ?")表达式可用@CronExpression("0/3 * * * * ?")代替
      
      2.2 基于Job接口
      新建任意类, 如DemoTask.java, 实现Job接口, 实现execute方法, 类上添加@Component注解
      -- 示例
      import com.cg.quartz.annotaion.ConcurrentExecution;
      import com.cg.quartz.annotaion.CronExpression;
      import com.cg.quartz.annotaion.TaskLog;
      import lombok.extern.slf4j.Slf4j;
      import org.quartz.Job;
      import org.quartz.JobExecutionContext;
      import org.quartz.JobExecutionException;
      
      @Slf4j
      @Component
      @CronExpression("0/3 * * * * ?")
      public class DemoTask implements Job {
      
          @Override
          public void execute(JobExecutionContext context) throws JobExecutionException {
              log.info("===DEMO TASK EXEC==");
          }
      }
      
      2.3 基于QuartzJobBean
       新建任意类, 如ExecTask.java, 继承QuartzJobBean类, 实现executeInternal方法, 类上添加@Component注解
      -- 示例
      import org.quartz.JobExecutionContext;
      import org.quartz.JobExecutionException;
      import org.springframework.scheduling.quartz.QuartzJobBean;
      import org.springframework.stereotype.Component;
      
      @Component
      public class ExecTask extends QuartzJobBean {
          
          @Override
          protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
              System.out.println("====ExecTask====");
          }
      }
   3. 启动定时任务
       3.1 启动类配置
       -- 非持久化模式(RAM模式)
       @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
       @ComponentScans({@ComponentScan("com.cg.quartz")})
       
       -- 持久化模式
       @SpringBootApplication
       @ComponentScans({@ComponentScan("com.cg.quartz")})
       @MapperScan(basePackages = {"com.cg.quartz.dao"})
       @EnablePersist
       3.2 系统参数式
           添加VM启动参数 -Druntask=true
       3.3 配置式(application.yml)
           启动配置文件中添加
           quartz:
             # 是否启用定时任务
             run-task: true
1.3 非持久化模式相关DDL
    -- 以下为Mysql DDL配置, 其他数据库(支持数据库同mybatis)可能需要更改相关语法
    -- 定时任务表
    CREATE TABLE `t_quartz_task` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `task_name` varchar(64) NOT NULL COMMENT '任务名(jobName, taskName)',
      `cron_expression` varchar(32) NOT NULL COMMENT 'cron表达式',
      `method` varchar(16) NOT NULL DEFAULT 'run' COMMENT '任务执行方法(默认是run, Job接口配置无效)',
      `allow_concurrent` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否允许任务并发执行(1:允许0:不允许)',
      `description` varchar(64) NOT NULL COMMENT '任务描述',
      `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '任务状态(是否启用任务:1:启用,0:关闭)',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_task_name` (`task_name`)
    ) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';
    -- 定时任务日志表
    CREATE TABLE `t_quartz_task_log` (
      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'PK',
      `type` varchar(6) NOT NULL COMMENT '日志等级(INFO, WARN, ERROR)',
      `thread` varchar(64) NOT NULL COMMENT '日志线程(执行task线程名)',
      `class_name` varchar(64) NOT NULL COMMENT '任务类名(全路径)',
      `task` varchar(32) NOT NULL COMMENT '任务名',
      `content` varchar(2048) NOT NULL COMMENT '日志内容',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      PRIMARY KEY (`id`),
      KEY `idx_task` (`task`),
      KEY `idx_create_time` (`create_time`)
    ) ENGINE=InnoDB AUTO_INCREMENT=2867 DEFAULT CHARSET=utf8mb4;
1.4 配置介绍
  --为什么还有配置项? 不是说无配置文件吗?
  -- 答: 此处配置为整个Quartz的全局配置, 不针对某个任务配置， 仅首次启用配置即可
         仅一个配置项需要手动配置, 其余均有默认值, 需要时配置即可
  1 配置项(以yml为例)
    quartz:
      run-task: true
      enable-persist: true
      enable-rmi: true
      enable-async-log: true
      handle-dissociate: always
    run-task: 
        含义: 是否启用定时任务
        取值: true/false
        默认值: false
        是否必选: 是
        相关属性(配置):等同VM属性 -Druntask=true/false
    enable-persist:
        含义: 是否启用持久化
        取值: true/false
        默认值: false
        是否必选: 否
        相关属性(配置):等同@EnablePersist注解
        备注: 启用该注解必须有正常数据库连接, 并建立相关DDL(见1.3 t_quartz_task)
     enable-rmi:
        含义: 是否启用RMI(可用于调用TaskManagerApi下接口)
        取值: true/false
        默认值: false
        是否必选: 否
        备注: 通过该接口可实时管理定时任务(可关闭), ConfigAutoRefreshTask为默认定时任务管理器
     enable-async-log:
        含义: 是否启用异步日志
        取值: true/false
        默认值: true
        是否必选: 否
        相关属性(配置): 相关用法见@TaskLog
        备注: 未配置相关DDL(见1.3 t_quartz_task_log), 则等同于普通log。定时任务开关状态受enable-persist控制
     handle-dissociate:
        含义: 处理游离任务
        取值: random/always
        默认值: random
        是否必选: 否
        相关属性(配置): 相关用法见@HandleDissociate
        备注: 在持久化模式下处理内存任务与数据库任务因异常情况导致不同步状况
  2 logback.xml
    定时任务日志配置, 当原有的项目有存在日志配置时, 此日志配置文件会失效, 此时以项目中的日志配置文件为准
  3 quartz.properties
    定时任务线程及其相关配置, 项目中可拷贝一份配置文件, 放于resources目录下, 此时自动覆盖quartz中的配置
1.5 常用注解, 类, 接口介绍
    @Task
        作用: 标识任务类
        用法: method:指定任务运行的方法(默认run方法); allowConcurrent:是否允许并行执行; cron: 定时任务表达式; description:任务描述
    @CronExpression
        作用: 标识cron表达式
        备注: 持久化模式下失效, 以数据库配置为准
    未完待续....
</pre>

