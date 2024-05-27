package org.wyh.gateway.core.filter.pre.flowcontrol;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.FilterProcessingException;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;

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
        //限流的类型，可以只对服务调用路径进行限流，也可以对整个服务进行限流
        private String type;
        //限流的模式，单机或分布式
        private String mode;
        //下面这两个量通常配合使用，来控制单位时间内的最大访问次数，即qps。
        //基本时间间隔
        private int duration;
        //基本时间间隔内的最大访问次数
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
            FlowCtrlFilter.Config filterConfig = (FlowCtrlFilter.Config)args[0];
            if(filterConfig == null){
                log.warn("【流量控制过滤器】未设置配置信息");
            }else{
                FlowCtrlExecutor flowCtrlExecutor = FlowCtrlExecutor.getInstance();
                //当前服务调用的路径
                String path = ctx.getAttribute(AttributeKey.HTTP_INVOKER).getInvokerPath();
                //当前访问服务的唯一id
                String uniqueId = ctx.getUniqueId();
                if(filterConfig.getType().equals(FLOW_CTRL_TYPE_PATH)){
                    //对路径限流，则限流对象的值为path
                    flowCtrlExecutor.doFlowCtrlFilter(filterConfig, path);
                }else if(filterConfig.getType().equals(FLOW_CTRL_TYPE_SERVICE)){
                    //对服务限流，则限流对象的值为uniqueId
                    flowCtrlExecutor.doFlowCtrlFilter(filterConfig, uniqueId);
                }else{
                    log.warn("【流量控制过滤器】不支持该限流策略: {}", filterConfig.getType());
                }
            }
        }catch (Exception e){
            log.error("【流量控制过滤器】过滤器执行异常", e);
            //过滤器执行过程出现异常，（正常）过滤器链执行结束，将上下文状态设置为terminated
            ctx.setTerminated();
            throw new FilterProcessingException(e, FLOW_CTRL_FILTER_ID, ResponseCode.FILTER_PROCESSING_ERROR);
        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，
             * 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx, args);
        }
    }
}
