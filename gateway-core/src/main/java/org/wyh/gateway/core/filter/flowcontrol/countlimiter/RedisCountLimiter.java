package org.wyh.gateway.core.filter.flowcontrol.countlimiter;

import org.wyh.gateway.common.utils.JedisUtil;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 15:10
 * @Description: 基于redis实现的（分布式）限流器，适用于以集群方式部署网关系统的情况。
                 本质上来说，该限流器是通过在redis中运行相应的lua脚本来实现的（lua脚本中包含了多条redis命令）。
                 redis提供的lua脚本功能能够确保多条命令执行的原子性，进而保证了该限流器是多线程安全的。
 */
public class RedisCountLimiter {
    //Jedis工具类实例。Jedis是java连接redis数据库的工具，类似于jdbc和关系型数据库的关系。
    protected JedisUtil jedisUtil;
    //表示不需要限流（放行请求）的常量
    private static final int SUCCESS_RESULT = 1;
    //表示需要限流（不放行请求）的常量
    private static final int FAILED_RESULT = 0;
    /**
     * @date: 2024-02-29 10:40
     * @description: 有参构造器
     * @Param jedisUtil:
     * @return: null
     */
    public RedisCountLimiter(JedisUtil jedisUtil){
        this.jedisUtil = jedisUtil;
    }
    /**
     * @date: 2024-02-29 10:43
     * @description: 判断是否需要进行限流
     * @Param key: 限流资源对象的值
     * @Param limit: 最大访问次数
     * @Param expire: 过期/到期时间。与上述属性配合，来控制一段时间内的最大访问次数（也就是流量控制）。
     * @return: boolean
     */
    public boolean doFlowCtrl(String key, int limit, int expire) {
        try{
            //在redis中执行lua脚本，通过返回结果判断该次请求是否需要限流。
            Object object = jedisUtil.executeScript(key, limit, expire);
            //返回结果为null，说明该请求访问的资源对象没有限流配置，因此不进行限流
            if (object == null) {
                return true;
            }
            Long result = Long.valueOf(object.toString());
            //需要限流
            if(result == FAILED_RESULT){
                return false;
            }
        }catch (Exception e){
            throw new RuntimeException("分布式限流发生错误");
        }
        //不需要限流
        return true;
    }
}
