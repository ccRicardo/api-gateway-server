package org.wyh.gateway.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.utils.NetUtils;
import org.wyh.gateway.common.utils.TimeUtil;
import org.wyh.gateway.config.api.ConfigCenter;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.netty.Container;
import org.wyh.gateway.register.api.RegisterCenter;
import org.wyh.gateway.register.api.RegisterCenterListener;

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
     * @description: 网关系统启动方法
     * @Param args:
     * @return: void
     */
    public static void main(String[] args) {
        //一、加载核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        log.info("api网关系统端口号：{}", config.getPort());
        //二、过滤插件初始化（插件初始化工作实际上是在过滤器链工厂类中完成的）
        //三、初始化配置中心，然后监听规则配置的变更，并保存最新的规则配置
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建配置中心实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * ServiceLoader.findFirst方法返回的是第一个（种）实现类的实例。
         */
        ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);
        final ConfigCenter configCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("未找到配置中心实例");
            return new RuntimeException("未找到配置中心实例");
        });
        configCenter.init(config.getConfigAddress(), config.getEnv());
        //RulesChangeListener是一个函数式接口，所以可以用lambda表达式来实现。其中rules代表新的规则配置列表。
        configCenter.subscribeRulesChange(rules -> {
            DynamicConfigManager.getInstance().putAllRule(rules);
        });
        //四、启动网关容器
        Container container = new Container(config);
        container.start();
        //五、初始化注册中心，并监听服务的变更
        final RegisterCenter registerCenter = registerAndSubscribe(config);
        //六、服务优雅关机
        //addShutdownHook添加的线程会在JVM关闭过程中被调用，因此通常用于在程序终止时执行一些清理工作。
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                //在注册中心注销掉网关服务。
                registerCenter.deregister(buildGatewayServiceDefinition(config),
                        buildGatewayServiceInstance(config));
            }
        });
    }
    /**
     * @date: 2024-01-23 10:33
     * @description: 初始化注册中心，并注册网关服务，然后监听服务的变更
                     （网关服务也需要注册到注册中心）
     * @Param config:
     * @return: org.wyh.gateway.register.api.RegisterCenter
     */
    private static RegisterCenter registerAndSubscribe(Config config){
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建注册中心实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * ServiceLoader.findFirst方法返回的是第一个（种）实现类的实例。
         */
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        final RegisterCenter registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("未找到注册中心实例");
            return new RuntimeException("未找到注册中心实例");
        });
        //初始化注册中心
        registerCenter.init(config.getRegistryAddress(), config.getEnv());
        //构造网关服务的服务定义和服务实例
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);
        //注册网关服务
        registerCenter.register(serviceDefinition, serviceInstance);
        //订阅所有服务。此处通过匿名内部类的方式实现了监听器接口中的方法。
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
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
        });
        return registerCenter;
    }
    /**
     * @date: 2024-01-23 10:41
     * @description: 构造网关服务的服务定义
    （网关服务也需要注册到注册中心）
     * @Param config:
     * @return: org.wyh.common.config.ServiceDefinition
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
     * @date: 2024-01-23 10:36
     * @description: 构造网关服务的服务实例
                     （网关服务也需要注册到注册中心）
     * @Param config: 
     * @return: org.wyh.common.config.ServiceInstance
     */
    private static ServiceInstance buildGatewayServiceInstance(Config config){
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        ServiceInstance serviceInstance = new ServiceInstance();
        //服务实例id的形式：ip:port
        serviceInstance.setServiceInstanceId(localIp + ":" + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }
}
