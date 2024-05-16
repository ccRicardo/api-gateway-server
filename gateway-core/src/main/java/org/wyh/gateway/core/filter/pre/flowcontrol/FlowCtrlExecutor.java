package org.wyh.gateway.core.filter.pre.flowcontrol;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.filter.common.chainfactory.GatewayFilterChainFactory;
import org.wyh.gateway.core.redis.JedisUtil;
import org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter.GuavaCountLimiter;
import org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter.RedisCountLimiter;

import java.util.concurrent.ConcurrentHashMap;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 14:16
 * @Description: 流量控制的实际执行器。目前支持两种限流的类型。
 */
@Slf4j
public class FlowCtrlExecutor{
    //限流时的提示信息
    private static final String LIMIT_MESSAGE ="您的请求过于频繁，请稍后重试";
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.flowcontrol
     * @Author: wyh
     * @Date: 2024-05-16 19:00
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder {
        private static final FlowCtrlExecutor INSTANCE = new FlowCtrlExecutor();
    }
    /**
     * @date: 2024-02-28 9:22
     * @description: private修饰的无参构造器
     * @return: null
     */
    private FlowCtrlExecutor(){
    }
    /**
     * @date: 2024-05-16 19:06
     * @description: 获取该类的唯一实例
     * @return: org.wyh.gateway.core.filter.pre.flowcontrol.FlowCtrlExecutor
     */
    public static FlowCtrlExecutor getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-16 19:07
     * @description: 按照指定的配置信息，对目标对象进行流量控制。
     * @Param flowCtrlConfig:
     * @Param value: 限流对象的值。若限流类型为path，则该值就是方法调用的路径；若为service，就是服务的唯一id。
     * @return: void
     */
    public void doFlowCtrlFilter(FlowCtrlFilter.Config flowCtrlConfig, String value) {
        //设定的时间间隔长度（以秒为单位）
        int duration = flowCtrlConfig.getDuration();
        //在上述时间间隔内，设定的最大访问次数
        int permits = flowCtrlConfig.getPermits();
        //标识是否正常处理该次请求（也就是不进行限流）
        boolean flag = true;
        //如果限流模式为分布式，则使用基于redis的分布式限流器；若为单机模式，使用基于guava的单机限流器。
        if(FLOW_CTRL_MODE_DISTRIBUTED.equals(flowCtrlConfig.getMode())){
            flag = RedisCountLimiter.getInstance().doFlowCtrl(value, duration, permits);
        }else if(FLOW_CTRL_MODE_SINGLETON.equals(flowCtrlConfig.getMode())){
            flag = GuavaCountLimiter.getInstance().doFlowCtrl(value,duration, permits);
        }else{
            log.warn("【流量控制过滤器】不支持该限流模式: {}", flowCtrlConfig.getMode());
        }
        if(!flag){
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
