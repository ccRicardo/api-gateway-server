package org.wyh.gateway.register.api;

import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;

import java.util.Set;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.register.center.api
 * @Author: wyh
 * @Date: 2024-01-23 9:50
 * @Description: 注册中心监听器接口。用于定义各种监听器。
 */
public interface RegisterCenterListener {
    /**
     * @date: 2024-01-23 9:53
     * @description: 用于监听服务的变动，本质上是一个回调函数，服务发生变动时调用
     * @Param serviceDefinition:
     * @Param serviceInstanceSet:
     * @return: void
     */
    void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet);
}
