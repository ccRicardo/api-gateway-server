package org.wyh.gateway.core.filter.pre.flowcontrol;

import lombok.extern.slf4j.Slf4j;
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
    //限流对象的值。若限流类型为path，则该值就是方法调用的路径；若为service，就是服务的唯一id。
    private String value;
    //基于redis的分布式限流器
    private RedisCountLimiter redisCountLimiter;
    //限流时的提示信息
    private static final String LIMIT_MESSAGE ="您的请求过于频繁，请稍后重试";
    /*
     * 保存限流对象值与对应的FlowCtrlExecutor对象
     * 这么做的目的是避免对同一个限流对象，创建多个重复的FlowCtrlExecutor对象。
     * 即大幅减少了创建FlowCtrlExecutor对象的开销。
     */
    private static ConcurrentHashMap<String, FlowCtrlExecutor> valueExecutorMap =
            new ConcurrentHashMap<>();
    /**
     * @date: 2024-02-28 9:22
     * @description: 有参构造器
     * @Param value:
     * @Param redisCountLimiter:
     * @return: null
     */
    public FlowCtrlExecutor(String value, RedisCountLimiter redisCountLimiter){
        this.value = value;
        this.redisCountLimiter = redisCountLimiter;
    }
    /**
     * @date: 2024-02-28 9:20
     * @description: 根据限流对象的值获取相应的FlowCtrlExecutor对象。
                     该方法可以避免重复创建FlowCtrlExecutor对象。
     * @Param path: 
     * @return: org.wyh.core.filter.flowcontrol.FlowCtrlExecutor
     */
    public static FlowCtrlExecutor getInstance(String value){
        FlowCtrlExecutor flowCtrlExecutor = valueExecutorMap.get(value);
        if(flowCtrlExecutor == null){
            flowCtrlExecutor = new FlowCtrlExecutor(value,
                    new RedisCountLimiter(new JedisUtil()));
            //将该value的FlowCtrlExecutor对象存入集合中，供之后使用
            valueExecutorMap.put(value, flowCtrlExecutor);
        }
        return flowCtrlExecutor;
    }
    /**
     * @date: 2024-05-16 16:56
     * @description: 按照指定的配置信息，对目标对象进行流量控制。
     * @Param flowCtrlConfig:
     * @return: void
     */
    public void doFlowCtrlFilter(FlowCtrlFilter.Config flowCtrlConfig) {
        //设定的时间间隔长度（以秒为单位）
        int duration = flowCtrlConfig.getDuration();
        //在上述时间间隔内，设定的最大访问次数
        int permits = flowCtrlConfig.getPermits();
        //标识是否正常处理该次请求（也就是不进行限流）
        boolean flag = true;
        //将限流对象的值作为redis key。
        String key = value;
        //如果限流模式为分布式，则使用基于redis的分布式限流器；若为单机模式，使用基于guava的单机限流器。
        if(FLOW_CTRL_MODE_DISTRIBUTED.equals(flowCtrlConfig.getMode())){
            flag = redisCountLimiter.doFlowCtrl(key, permits, duration);
        }else if(FLOW_CTRL_MODE_SINGLETON.equals(flowCtrlConfig.getMode())){
            //计算每秒的最大访问数，向下取整
            int maxPermits = permits / duration;
            //获取该限流对象对应的GuavaCountLimiter实例
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(value, maxPermits);
            //通常每个请求消耗一个令牌
            flag = guavaCountLimiter.acquire(1);
        }else{
            log.warn("【流量控制过滤器】不支持该限流模式: {}", flowCtrlConfig.getMode());
        }
        if(!flag){
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
