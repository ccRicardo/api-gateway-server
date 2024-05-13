package org.wyh.gateway.core.filter.flowcontrol;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.Filter;
import org.wyh.gateway.core.filter.common.FilterAspect;

import java.util.Iterator;
import java.util.Set;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 10:32
 * @Description: 流量控制/限流过滤器
 */
@Slf4j
@FilterAspect(id=FLOW_CTRL_FILTER_ID,
              name=FLOW_CTRL_FILTER_NAME,
              order=FLOW_CTRL_FILTER_ORDER)
public class FlowCtrlFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        IGatewayFlowCtrlRule flowCtrlRule = null;
        Rule rule = ctx.getRule();
        if(rule != null){
            //获取流量控制配置集合
            Set<Rule.FlowCtrlConfig> flowCtrlConfigs = rule.getFlowCtrlConfigs();
            Iterator<Rule.FlowCtrlConfig> iterator = flowCtrlConfigs.iterator();
            Rule.FlowCtrlConfig flowCtrlConfig;
            String path;
            String uniqueId;
            //遍历流量控制配置集合，找到与当前上下文匹配的流量控制配置项
            while(iterator.hasNext()){
                flowCtrlConfig = iterator.next();
                if(flowCtrlConfig == null){
                    continue;
                }
                path = ctx.getRequest().getPath();
                uniqueId = ctx.getUniqueId();
                //如果流量控制类型为路径，则比较配置值和当前的路径值，类型为服务id时同理。
                if(flowCtrlConfig.getType().equals(FLOW_CTRL_TYPE_PATH)
                        && path.equals(flowCtrlConfig.getValue())){
                    //根据path获取相应的路径限流策略实例
                    flowCtrlRule = FlowCtrlByPathRule.getInstance(path);
                }else if(flowCtrlConfig.getType().equals(FLOW_CTRL_TYPE_SERVICE)
                        && uniqueId.equals(flowCtrlConfig.getValue())){
                    //根据serviceId获取相应的服务限流策略实例
                    flowCtrlRule = FlowCtrlByServiceRule.getInstance(uniqueId);
                }
                //若在本次循环中找到了相应的配置项，则调用流量控制策略实例的相应方法完成过滤，然后退出循环
                if(flowCtrlRule != null){
                    flowCtrlRule.doFlowCtrlFilter(flowCtrlConfig);
                    break;
                }
            }
        }
    }
}
