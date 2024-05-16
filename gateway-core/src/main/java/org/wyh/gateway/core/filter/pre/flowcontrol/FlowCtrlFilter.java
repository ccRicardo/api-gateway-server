package org.wyh.gateway.core.filter.pre.flowcontrol;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;

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
              type=FilterType.PRE,
              order=FLOW_CTRL_FILTER_ORDER)
public class FlowCtrlFilter extends AbstractGatewayFilter<FlowCtrlFilter.Config> {
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.flowcontrol
     * @Author: wyh
     * @Date: 2024-02-27 10:32
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig {
        //限流的类型，可以是路径，服务唯一id或者ip
        private String type;
        //限流的对象（的值）
        private String value;
        //限流的模式，单机或分布式
        private String mode;
        //下面这两个量通常配合使用，来控制单位时间内的最大访问次数，即qps。
        //基本时间区间
        private int duration;
        //基本时间区间内的最大访问次数
        private int permits;
    }
    /**
     * @date: 2024-05-16 15:26
     * @description: 无参构造器，负责初始化父类的filterConfigClass属性
     * @return: null
     */
    public FlowCtrlFilter(){
        super(FlowCtrlFilter.Config.class);
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        try{
            //args[0]其实就是该过滤器的配置类实例
            IGatewayFlowCtrlRule flowCtrlRule = null;
            Rule rule = ctx.getRule();
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
                    //根据uniqueId获取相应的服务限流策略实例
                    flowCtrlRule = FlowCtrlByServiceRule.getInstance(uniqueId);
                }
                //若在本次循环中找到了相应的配置项，则调用流量控制策略实例的相应方法完成过滤，然后退出循环
                if(flowCtrlRule != null){
                    flowCtrlRule.doFlowCtrlFilter(flowCtrlConfig);
                    break;
                }
            }
        }catch (){

        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx, args);
        }
    }
}
