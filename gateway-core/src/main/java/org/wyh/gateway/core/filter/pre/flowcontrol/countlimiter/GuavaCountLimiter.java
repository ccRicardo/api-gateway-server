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
                 本质上来说，该限流器就是对RateLimiter的包装，而RateLimiter是基于令牌桶实现的。

 */
public class GuavaCountLimiter implements CountLimiter{
    /*
     * 补充说明：
     * RateLimiter是一个基于令牌桶算法实现的限流器，此处用于控制服务的qps（即限流）。
     * 令牌桶可以视为一个存放令牌的容器，它预先设定了一定的容量。
     * 系统会按照设定速率向桶中放置令牌，当桶中令牌放满时，多余的令牌会溢出。
     * 当某个请求要访问服务时，需要先从令牌桶中取出一定数量（通常是1）的令牌。
     * 如果桶中令牌数量不够，那么该请求会被忽略或者放入缓冲队列中。
     */
    /*
     * 保存限流对象与对应的RateLimiter限流器实例。
     * 注意：若限流类型为path，则限流对象的值就是方法调用的路径；若为service，就是服务的唯一id
     */
    private static final ConcurrentHashMap<String, RateLimiter> limiterMap =
            new ConcurrentHashMap<>();
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.flowcontrol
     * @Author: wyh
     * @Date: 2024-05-16 20:05
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final GuavaCountLimiter INSTANCE = new GuavaCountLimiter();
    }
    /**
     * @date: 2024-05-16 20:05
     * @description: private修饰的无参构造器
     * @return: null
     */
    private GuavaCountLimiter(){
    }
    /**
     * @date: 2024-05-16 20:34
     * @description: 获取该类的唯一实例
     * @return: org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter.GuavaCountLimiter
     */
    public static GuavaCountLimiter getInstance(){
        return SingletonHolder.INSTANCE;
    }

    /**
     * @date: 2024-02-28 15:00
     * @description: 根据限流对象的值从limiterMap中获取对应的RateLimiter限流器实例。
     * @Param value:
     * @Param permitsPerSecond:
     * @return: org.wyh.core.filter.flowcontrol.GuavaCountLimiter
     */
    private RateLimiter getLimiter(String value, int permitsPerSecond){
        RateLimiter limiter = limiterMap.get(value);
        if(limiter == null){
            limiter = RateLimiter.create(permitsPerSecond);
            //将该限流对象的RateLimiter实例存入limiterMap中，供之后使用
            limiterMap.put(value, limiter);
        }else{
            //注意：需要检查该限流对象设定的qps是否发生变更
            if(limiter.getRate() != permitsPerSecond){
                limiter.setRate(permitsPerSecond);
            }
        }
        return limiter;
    }
    @Override
    public boolean doFlowCtrl(String value, int duration, int permits){
        //该方法实际上就是在尝试从对应的RateLimiter令牌桶中获取一个令牌
        //计算每秒的最大允许访问次数，向下取整（也就是qps）
        int permitsPerSecond = permits / duration;
        //根据限流对象的值获取对应的RateLimiter实例
        RateLimiter limiter = getLimiter(value, permitsPerSecond);
        //标识令牌获取是否成功。true表示令牌获取成功，即正常处理该次请求，不进行限流。
        boolean flag = limiter.tryAcquire(1);
        return flag;
    }

}
