package org.wyh.gateway.core.manager;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.utils.NetUtils;
import org.wyh.gateway.common.utils.TimeUtil;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.register.api.RegisterCenter;
import org.wyh.gateway.register.api.RegisterCenterListener;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.manager
 * @Author: wyh
 * @Date: 2024-05-21 9:52
 * @Description: 注册中心管理类，
                 负责（通过SPI）加载注册中心实例，初始化注册中心，设置监听器和注册网关服务等。
                 注：注册网关服务指的是将网关服务注册到注册中心
 */
@Slf4j
public class RegisterCenterManager {
    //注册中心实例
    private static RegisterCenter registerCenter;
    //注册中心监听器的默认实现
    public static final RegisterCenterListener DEFAULT_LISTENER = new RegisterCenterListener() {
        @Override
        public void onServicesChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
            // TODO: 2024-05-21 可以加个流操作，只打印每个实例的id
            log.info("更新服务: {}的实例: {}", serviceDefinition.getUniqueId(), serviceInstanceSet);
            //获取动态配置管理器的实例
            DynamicConfigManager manager = DynamicConfigManager.getInstance();
            //将新的服务定义缓存到本地
            manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
            //将新的服务实例缓存到本地。
            manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);
        }
    };
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.manager
     * @Author: wyh
     * @Date: 2024-05-21 10:20
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final RegisterCenterManager INSTANCE = new RegisterCenterManager();
    }
    /**
     * @date: 2024-05-21 10:20
     * @description: private修饰的无参构造器
     * @return: null
     */
    private RegisterCenterManager(){}
    public static RegisterCenterManager getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-21 10:31
     * @description: 负责通过spi加载注册中心实例，调用其初始化方法以及注册网关服务（将网关服务注册到注册中心）
     * @Param config:
     * @return: void
     */
    public void init(Config config){
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
        //初始化
        registerCenter.init(config.getRegistryAddress(), config.getEnv());
        //注册网关服务
        registerCenter.register(buildGatewayServiceDefinition(config),
                buildGatewayServiceInstance(config));
    }
    /**
     * @date: 2024-05-21 21:53
     * @description: 根据静态配置信息，构建网关的服务定义
     * @Param config: 
     * @return: org.wyh.gateway.common.config.ServiceDefinition
     */
    private ServiceDefinition buildGatewayServiceDefinition(Config config){
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        //Map.of方法创建的是一个不可修改的空Map
        serviceDefinition.setInvokerMap(Map.of());
        //网关服务的唯一id和服务id就是配置文件中设定的applicationName
        serviceDefinition.setUniqueId(config.getApplicationName()+":"+config.getVersion());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setEnvType(config.getEnv());
        serviceDefinition.setDesc("api网关服务");
        return serviceDefinition;
    }
    /**
     * @date: 2024-05-21 21:56
     * @description: 根据静态配置信息，构建网关的服务实例
     * @Param config: 
     * @return: org.wyh.gateway.common.config.ServiceInstance
     */
    private ServiceInstance buildGatewayServiceInstance(Config config){
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
    /**
     * @date: 2024-05-21 10:32
     * @description: 根据传入的注册中心监听器，订阅注册中心的服务变更
     * @Param listener:
     * @return: void
     */
    public void subscribeRegisterCenter(RegisterCenterListener listener){
        registerCenter.subscribeServicesChange(listener);
    }


}
