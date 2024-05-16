package org.wyh.gateway.core.filter.pre.flowcontrol;

import org.wyh.gateway.common.config.Rule;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 10:31
 * @Description: 流量控制策略（规则）的顶级接口
 */
public interface IGatewayFlowCtrlRule {
    /**
     * @date: 2024-02-27 14:11
     * @description: 根据配置信息，执行流量控制过滤
     * @Param flowCtrlConfig:
     * @return: void
     */
    void doFlowCtrlFilter(Rule.FlowCtrlConfig flowCtrlConfig);
}
