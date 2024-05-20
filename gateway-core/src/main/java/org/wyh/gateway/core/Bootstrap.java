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
import org.wyh.gateway.config.api.RulesChangeListener;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.config.ConfigLoader;
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
    //配置中心，通过静态代码块初始化
    private static final ConfigCenter configCenter;
    //注册中心，通过静态代码块初始化
    private static final RegisterCenter registerCenter;
    //网关的规则变更监听器实例，通过静态代码块初始化
    private static final RulesChangeListener gatewayRulesChangeListener;
    //网关的注册中心监听器实例，用于监听服务的变更，通过静态代码块初始化
    private static final RegisterCenterListener gatewayRegisterCenterListener;

    /**
     * @date: 2024-01-23 10:30
     * @description: 核心方法，负责启动整个网关系统
     * @Param args:
     * @return: void
     */
    public static void main(String[] args) {
        //一、加载核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        log.info("api网关系统端口号：{}", config.getPort());
        //二、过滤插件初始化（插件初始化工作实际上是在过滤器链工厂类中完成的）
        //三、初始化配置中心，然后设置RulesChangeListener监听器实例，订阅规则的变更
        configCenter.init(config.getConfigAddress(), config.getEnv());
        configCenter.subscribeRulesChange(gatewayRulesChangeListener);
        //四、启动网关容器
        Container container = new Container(config);
        container.start();
        //五、初始化注册中心，然后注册网关服务，最后设置RegisterCenterListener监听器实例，订阅服务的变更
        registerCenter.init(config.getRegistryAddress(), config.getEnv());
        registerCenter.register(buildGatewayServiceDefinition(config),
                buildGatewayServiceInstance(config));
        registerCenter.subscribeAllServices(gatewayRegisterCenterListener);
        //六、服务优雅关机
        //addShutdownHook添加的线程会在JVM关闭之前被调用，因此通常用于在程序终止时执行一些清理工作。
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                //在注册中心注销掉网关服务。
                registerCenter.deregister(buildGatewayServiceDefinition(config),
                        buildGatewayServiceInstance(config));
                //关闭网关容器
                container.shutdown();
            }
        });
    }
    //静态代码块，负责初始化配置中心，注册中心，规则变更监听器和注册中心监听器属性
    static{
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建配置中心实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * ServiceLoader.findFirst方法返回的是第一个（种）实现类的实例。
         */
        ServiceLoader<ConfigCenter> configCenters = ServiceLoader.load(ConfigCenter.class);
        configCenter = configCenters.findFirst().orElseThrow(() -> {
            //如果没找到实现类，则执行以下lambda表达式，抛出异常
            log.error("未找到配置中心实例");
            return new RuntimeException("未找到配置中心实例");
        });
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建注册中心实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * ServiceLoader.findFirst方法返回的是第一个（种）实现类的实例。
         */
        ServiceLoader<RegisterCenter> registerCenters = ServiceLoader.load(RegisterCenter.class);
        registerCenter = registerCenters.findFirst().orElseThrow(() -> {
            //如果没找到实现类，则执行以下lambda表达式，抛出异常
            log.error("未找到注册中心实例");
            return new RuntimeException("未找到注册中心实例");
        });
        /*
         * 初始化规则变更监听器
         */
        gatewayRulesChangeListener = new RulesChangeListener() {
            @Override
            public void onRulesChange(List<Rule> rules) {
                //rules代表新的规则配置列表。
                DynamicConfigManager.getInstance().putAllRule(rules);
            }
        };
        /*
         * 初始化注册中心监听器
         */
        gatewayRegisterCenterListener = new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
                //日志输出
                log.info("更新服务定义和服务实例: {} {}", serviceDefinition.getUniqueId(),
                        JSON.toJSON(serviceInstanceSet));
                //获取动态配置管理器的实例
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                //将新的服务定义缓存到本地
                manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
                //将新的服务实例缓存到本地。
                manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);
            }
        };
    }
    /*
     * 以下是一些private修饰的辅助方法
     */
    /**
     * @date: 2024-05-20 17:01
     * @description: 根据静态配置信息，构建网关的服务定义
     * @Param config:
     * @return: org.wyh.gateway.common.config.ServiceDefinition
     */
    private static ServiceDefinition buildGatewayServiceDefinition(Config config){
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        //Map.of方法创建的是一个不可修改的空Map
        serviceDefinition.setInvokerMap(Map.of());
        //网关服务的唯一id和服务id就是配置文件中设定的applicationName
        serviceDefinition.setUniqueId(config.getApplicationName()+":"+config.getVersion());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setEnvType(config.getEnv());
        return serviceDefinition;
    }
    /**
     * @date: 2024-05-20 17:02
     * @description: 根据静态配置信息，构建网关的服务实例
     * @Param config:
     * @return: org.wyh.gateway.common.config.ServiceInstance
     */
    private static ServiceInstance buildGatewayServiceInstance(Config config){
        ServiceInstance serviceInstance = new ServiceInstance();
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        //服务实例id的形式：ip:port
        serviceInstance.setServiceInstanceId(localIp + ":" + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }
}
