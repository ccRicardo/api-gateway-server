package org.wyh.gateway.register.api;

import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.register.center.api
 * @Author: wyh
 * @Date: 2024-01-23 9:42
 * @Description: 注册中心接口定义
 */
public interface RegisterCenter {
    /**
     * @date: 2024-01-23 9:54
     * @description: 注册中心初始化
     * @Param registerAddress:
     * @Param env:
     * @return: void
     */
    void init(String registerAddress, String env);
    /**
     * @date: 2024-01-23 9:54
     * @description: 注册服务实例
     * @Param serviceDefinition:
     * @Param serviceInstance:
     * @return: void
     */
    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);
    /**
     * @date: 2024-01-23 9:55
     * @description: 注销服务实例
     * @Param serviceDefinition:
     * @Param serviceInstance:
     * @return: void
     */
    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);
    /**
     * @date: 2024-01-23 9:55
     * @description: 订阅所有服务，即监听所有服务实例的更新
     * @Param listener: 
     * @return: void
     */
    void subscribeAllServices(RegisterCenterListener listener);
}
