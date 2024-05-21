package org.wyh.gateway.config.api;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.config.center.api
 * @Author: wyh
 * @Date: 2024-01-30 14:36
 * @Description: 配置中心接口类。配置中心主要用来存放网关的动态规则配置。
 */
public interface ConfigCenter {
    /**
     * @date: 2024-01-30 14:37
     * @description: 配置中心初始化
     * @Param serverAddr: 配置中心的服务器地址
     * @Param env:
     * @return: void
     */
    void init(String serverAddr, String env);
    /**
     * @date: 2024-01-30 14:39
     * @description: 订阅/监听规则配置的变更。（配置中心主要就是用来存放网关的规则配置）
     * @Param listener: 配置中心监听器。这是一个函数式接口，可以用lambda表达式来实现。
     * @return: void
     */
    void subscribeRulesChange(ConfigCenterListener listener);
}
