package org.wyh.gateway.core.filter.pre.loadbalance;

import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.core.context.GatewayContext;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
 * @Author: wyh
 * @Date: 2024-05-22 13:52
 * @Description: 负载均衡的顶级接口
 */
public interface LoadBalance {
    //默认的服务实例标准权重
    int DEFAULT_WEIGHT = 100;
    //默认的服务实例预热时间，单位为ms（服务实例的预热期间，其权重会随时间线性增加）
    int DEFAULT_WARMUP_MS = 3 * 60 * 1000;
    /**
     * @date: 2024-05-22 13:59
     * @description: 根据具体的负载均衡策略，从服务实例列表中选择一个实例
                     （服务实例列表可以从上下文的参数中获取）
     * @Param ctx:
     * @return: org.wyh.gateway.common.config.ServiceInstance
     */
    ServiceInstance select(GatewayContext ctx);
}
