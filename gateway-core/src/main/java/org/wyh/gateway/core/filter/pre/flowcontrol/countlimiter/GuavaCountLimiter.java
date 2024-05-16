package org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 15:04
 * @Description: 基于Guava实现的（单机）限流器，适用于以单机模式部署网关系统的情况。
                 本质上来说，该限流器是通过RateLimiter的令牌桶来实现的。

 */
// TODO: 2024-03-05 经测试发现，该限流器的效果不如redis限流器好，准确。目前原因未知，在测试时需要注意。
public class GuavaCountLimiter {
    /*
     * RateLimiter是一个基于令牌桶算法实现的限流器，此处用于控制服务的qps（即限流）。
     * 令牌桶可以视为一个存放令牌的容器，它预先设定了一定的容量。
     * 系统会按照设定速率向桶中放置令牌，当桶中令牌放满时，多余的令牌会溢出。
     * 当某个请求要访问服务时，需要先从令牌桶中取出一定数量（通常是1）的令牌。
     * 如果桶中令牌数量不够，那么该请求会被忽略或者放入缓冲队列中。
     */
    private RateLimiter rateLimiter;
    //每秒的最大允许访问数
    private int maxPermits;
    /*
     * 保存被限流的资源对象（路径/服务id）与对应的限流器实例。注意，key可能是路径，也可能是服务id。
     * 这么做的目的是避免对同一个限流对象，创建多个重复的GuavaCountLimiter对象。
     * 即大幅减少了创建GuavaCountLimiter对象的开销。
     */
    private static ConcurrentHashMap<String, GuavaCountLimiter> resourceLimiterMap =
            new ConcurrentHashMap<>();
    /*
     * 以下两个构造器创建的是不同类型的RateLimiter实现类对象。
     * 前者创建的是SmoothBursty类型的对象。该对象实现了一个基本的令牌桶算法，令牌生成速率始终是恒定的。
     * 后者创建的是SmoothWarmingUp类型的对象。该对象在SmoothBursty基础上增加了一个预热期。
     * 在预热期内，令牌的生成速率会逐渐增加，最终达到一个稳定的速率。
     * 因此，SmoothWarmingUp更适用于资源需要预热，处理流量逐渐增加的场景。
     */
    /**
     * @date: 2024-02-28 14:30
     * @description: 有参构造器，创建的是一个SmoothBursty（RateLimiter实现类）类型的对象。
     * @Param maxPermits:
     * @return: null
     */
    public GuavaCountLimiter(int maxPermits){
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits);
    }
    /**
     * @date: 2024-02-28 14:32
     * @description: 有参构造器，创建的是一个SmoothWarmingUp（RateLimiter实现类）类型的对象。
     * @Param maxPermits:
     * @Param warmupPeriod: 预热时间
     * @return: null
     */
    public GuavaCountLimiter(int maxPermits, long warmupPeriod){
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits, warmupPeriod, TimeUnit.SECONDS);
    }
    /**
     * @date: 2024-02-28 15:00
     * @description: 根据限流配置获取对应的GuavaCountLimiter实例。
                     该方法可以避免重复创建GuavaCountLimiter对象。
     * @Param flowCtrlConfig:
     * @Param maxPermits:
     * @return: org.wyh.core.filter.flowcontrol.GuavaCountLimiter
     */
    public static GuavaCountLimiter getInstance(Rule.FlowCtrlConfig flowCtrlConfig, int maxPermits){
        if(flowCtrlConfig == null || StringUtils.isEmpty(flowCtrlConfig.getConfig())){
            return null;
        }
        //被限流的资源对象（路径/服务id）的值
        String key = flowCtrlConfig.getValue();
        GuavaCountLimiter limiter = resourceLimiterMap.get(key);
        if(limiter == null){
            limiter = new GuavaCountLimiter(maxPermits);
            //将该限流对象的GuavaCountLimiter实例存入resourceLimiterMap中，供之后使用
            resourceLimiterMap.put(key, limiter);
        }
        return limiter;
    }
    /**
     * @date: 2024-02-28 15:23
     * @description: 从令牌桶中获取指定数量的令牌
     * @Param permits:
     * @return: boolean
     */
    public boolean acquire(int permits){
        //标识令牌获取是否成功。true表示令牌获取成功，即不进行限流，返回true。
        boolean flag = rateLimiter.tryAcquire(permits);
        if(flag){
            return true;
        }
        return false;
    }

}
