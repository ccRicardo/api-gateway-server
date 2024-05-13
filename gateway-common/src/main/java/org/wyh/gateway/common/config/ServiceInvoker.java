package org.wyh.gateway.common.config;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.config
 * @Author: wyh
 * @Date: 2024-01-17 14:24
 * @Description: 服务方法（调用）的接口定义
                 方法调用指的是服务向外暴露/提供的可调用方法
 */

public interface ServiceInvoker {
    /**
     * @date: 2024-01-17 14:52
     * @description: 设置方法调用的全路径
     * @Param invokerPath:
     * @return: void
     */
    void setInvokerPath(String invokerPath);
    /**
     * @date: 2024-01-17 14:52
     * @description: 获取服务调用的全路径
     * @return: java.lang.String
     */
    String getInvokerPath();
    /**
     * @date: 2024-05-13 9:56
     * @description: 设置该方法调用绑定的规则id（一个服务可以有多个方法，每个方法都可以绑定不同的规则）
     * @Param ruleId:
     * @return: void
     */
    void setRuleId(String ruleId);
    /**
     * @date: 2024-05-13 9:57
     * @description: 获取该方法调用绑定的规则id
     * @return: java.lang.String
     */
    String getRuleId();
    /**
     * @date: 2024-01-17 14:57
     * @description: 设置该方法调用的超时时间
     * @Param timeout:
     * @return: void
     */
    void setTimeout(int timeout);
    /**
     * @date: 2024-01-17 14:57
     * @description: 获取该方法调用的超时时间
     * @return: int
     */
    int getTimeout();
    /**
     * @date: 2024-01-26 10:31
     * @description: 设置该方法的描述信息
     * @Param desc:
     * @return: void
     */
    void setDesc(String desc);
    /**
     * @date: 2024-01-26 10:31
     * @description: 获取该方法的描述信息
     * @return: java.lang.String
     */
    String getDesc();
}
