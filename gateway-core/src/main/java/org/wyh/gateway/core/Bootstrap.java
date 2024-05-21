package org.wyh.gateway.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.utils.NetUtils;
import org.wyh.gateway.common.utils.TimeUtil;
import org.wyh.gateway.config.api.ConfigCenter;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.manager.ConfigCenterManager;
import org.wyh.gateway.core.manager.RegisterCenterManager;
import org.wyh.gateway.core.netty.Container;
import org.wyh.gateway.register.api.RegisterCenter;
import org.wyh.gateway.register.api.RegisterCenterListener;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core
 * @Author: wyh
 * @Date: 2024-01-03 10:25
 * @Description: 网关启动/引导类
 */
@Slf4j
public class Bootstrap {
    /**
     * @date: 2024-01-23 10:30
     * @description: 核心方法，负责启动整个网关系统
     * @Param args:
     * @return: void
     */
    public static void main(String[] args) {
        //一、加载核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        log.info("api网关系统ip: {} 端口号: {}", NetUtils.getLocalIp(), config.getPort());
        //二、过滤插件初始化（插件初始化工作实际上是在过滤器链工厂类中完成的）
        // TODO: 2024-05-20 未完成过滤器插件的初始化
        //三、初始化配置中心，然后设置RulesChangeListener监听器实例，订阅规则的变更
        ConfigCenterManager configCenterManager = ConfigCenterManager.getInstance();
        configCenterManager.init(config);
        configCenterManager.subscribeConfigCenter(ConfigCenterManager.DEFAULT_LISTENER);
        //四、启动网关容器
        Container container = new Container(config);
        container.start();
        //五、初始化注册中心，然后设置RegisterCenterListener监听器实例，订阅服务的变更
        RegisterCenterManager registerCenterManager = RegisterCenterManager.getInstance();
        registerCenterManager.init(config);
        registerCenterManager.subscribeRegisterCenter(RegisterCenterManager.DEFAULT_LISTENER);
        //六、服务优雅关机
        //addShutdownHook添加的线程会在JVM关闭之前被调用，因此通常用于在程序终止时执行一些清理工作。
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                //关闭网关容器
                container.shutdown();
            }
        });
    }
}
