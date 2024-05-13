package org.wyh.gateway.core.filter.flowcontrol;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.core.redis.JedisUtil;
import org.wyh.gateway.core.filter.flowcontrol.countlimiter.GuavaCountLimiter;
import org.wyh.gateway.core.filter.flowcontrol.countlimiter.RedisCountLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 15:07
 * @Description: 通过服务id实现/完成流量控制。
 */
public class FlowCtrlByServiceRule implements IGatewayFlowCtrlRule{
    //服务唯一id
    private String uniqueId;
    //基于redis的分布式限流器
    private RedisCountLimiter redisCountLimiter;
    //限流时的提示信息
    private static final String LIMIT_MESSAGE ="您的请求过于频繁，请稍后重试";
    /*
     * 保存服务唯一id与对应的FlowCtrlByServiceRule对象
     * 这么做的目的是避免对同一个uniqueId，创建多个重复的FlowCtrlByServiceRule对象。
     * 即大幅减少了创建FlowCtrlByServiceRule对象的开销。
     */
    private static ConcurrentHashMap<String, FlowCtrlByServiceRule> serviceRuleMap =
            new ConcurrentHashMap<>();
    /**
     * @date: 2024-02-28 10:25
     * @description: 有参构造器
     * @Param uniqueId:
     * @Param redisCountLimiter:
     * @return: null
     */
    public FlowCtrlByServiceRule(String uniqueId, RedisCountLimiter redisCountLimiter){
        this.uniqueId = uniqueId;
        this.redisCountLimiter = redisCountLimiter;
    }
    /**
     * @date: 2024-02-28 10:26
     * @description: 根据uniqueId获取相应的FlowCtrlByServiceRule对象。
                     该方法可以避免重复创建FlowCtrlByServiceRule对象。
     * @Param uniqueId:
     * @return: org.wyh.core.filter.flowcontrol.FlowCtrlByServiceRule
     */
    public static FlowCtrlByServiceRule getInstance(String uniqueId){
        FlowCtrlByServiceRule flowCtrlByServiceRule = serviceRuleMap.get(uniqueId);
        if(flowCtrlByServiceRule == null){
            flowCtrlByServiceRule = new FlowCtrlByServiceRule(uniqueId,
                    new RedisCountLimiter(new JedisUtil()));
            //将该uniqueId的FlowCtrlByServiceRule对象存入serviceRuleMap中，供之后使用
            serviceRuleMap.put(uniqueId, flowCtrlByServiceRule);
        }
        return flowCtrlByServiceRule;
    }
    @Override
    public void doFlowCtrlFilter(Rule.FlowCtrlConfig flowCtrlConfig) {
        //如果未进行流量控制配置，则不做处理
        if(flowCtrlConfig == null || StringUtils.isEmpty(flowCtrlConfig.getConfig())){
            return;
        }
        /*
         * 配置信息是一个json串，这里将其转换成了一个map
         * 其中，key是配置项的名称，value是设定的值（这里值为整数类型）
         */
        Map<String, Integer> map = JSON.parseObject(flowCtrlConfig.getConfig(), Map.class);
        //若未同时配置duration和permits属性，则视为无效配置
        if(!map.containsKey(FLOW_CTRL_LIMIT_DURATION) || !map.containsKey(FLOW_CTRL_LIMIT_PERMITS)){
            return;
        }
        //设定的时间间隔长度（以秒为单位）
        int duration = map.get(FLOW_CTRL_LIMIT_DURATION);
        //在上述时间间隔内，设定的最大访问次数
        int permits = map.get(FLOW_CTRL_LIMIT_PERMITS);
        //标识是否对当前的请求进行限流处理（也就是是否抛弃/忽略该次请求）。true为正常处理该次请求，不进行限流。
        boolean flag = true;
        //限流对象的值就是uniqueId属性的值
        String key = uniqueId;
        //如果限流模式为分布式，则使用基于redis的分布式限流器；否则，使用基于guava的单机限流器。
        if(FLOW_CTRL_MODE_DISTRIBUTED.equals(flowCtrlConfig.getMode())){
            flag = redisCountLimiter.doFlowCtrl(key, permits, duration);
        }else{
            //计算每秒的最大访问数，向下取整
            int maxPermits = permits / duration;
            //根据uniqueId获取相应的GuavaCountLimiter实例
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(flowCtrlConfig, maxPermits);
            if(guavaCountLimiter == null){
                throw new RuntimeException("获取单机限流工具为空");
            }
            //通常每个请求消耗一个令牌
            flag = guavaCountLimiter.acquire(1);
        }
        //若需要对该请求进行限流，则直接抛出异常，不再继续进行过滤处理。反之，则直接放行。
        if(!flag){
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
