package org.wyh.gateway.core.netty;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core
 * @Author: wyh
 * @Date: 2024-01-15 15:45
 * @Description: 管理网关中各组件的生命周期
 */
public interface LifeCycle {
    /**
     * @date: 2024-01-15 15:46
     * @description: 组件初始化
     * @return: void
     */
    void init();
    /**
     * @date: 2024-01-15 15:47
     * @description: 启动组件
     * @return: void
     */
    void start();
    /**
     * @date: 2024-01-15 15:47
     * @description: 关闭组件
     * @return: void
     */
    void shutdown();
}
