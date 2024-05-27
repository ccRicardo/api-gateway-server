package org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter;

import org.wyh.gateway.core.jedis.JedisUtil;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.flowcontrol
 * @Author: wyh
 * @Date: 2024-02-27 15:10
 * @Description: 基于redis实现的（分布式）限流器，适用于以集群方式部署网关系统的情况。
                 本质上来说，该限流器是通过在redis中运行相应的lua脚本来实现的（lua脚本中包含了多条redis命令）。
                 redis提供的lua脚本功能能够确保多条命令执行的原子性，进而保证了该限流器是多线程安全的。
 */
public class RedisCountLimiter implements CountLimiter{
    //Jedis工具类实例。Jedis是java连接redis数据库的工具，类似于jdbc和关系型数据库的关系。
    private static final JedisUtil jedisUtil = new JedisUtil();
    //表示不需要限流（放行请求）的常量
    private static final int SUCCESS_RESULT = 1;
    //表示需要限流（不放行请求）的常量
    private static final int FAILED_RESULT = 0;
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.flowcontrol
     * @Author: wyh
     * @Date: 2024-05-16 20:30
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final RedisCountLimiter INSTANCE = new RedisCountLimiter();
    }
    /**
     * @date: 2024-05-16 20:32
     * @description: private修饰的无参构造器
     * @return: null
     */
    private RedisCountLimiter(){
    }
    /**
     * @date: 2024-05-16 20:35
     * @description: 获取该类的唯一实例
     * @return: org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter.RedisCountLimiter
     */
    public static RedisCountLimiter getInstance(){
        return SingletonHolder.INSTANCE;
    }
    @Override
    public boolean doFlowCtrl(String key, int expire, int limit) {
        /*
         * 参数意义的说明：
         * key: 指的是redis键值对中的key，其值等于限流对象的值
         * expire: 上述key的过期/到期时间。
         * limit: 上述时间内，key的最大访问次数
         * 注意：
         * 由于key有过期时间，并且通常都设置的很短，
         * 所以redis限流器并不需要检查其对应的限流对象的qps设定是否发生变更。
         */
        try{
            //在redis中执行lua脚本，通过返回结果判断该次请求是否需要限流。
            Object object = jedisUtil.executeScript(key, limit, expire);
            Long result = Long.valueOf(object.toString());
            //需要限流
            if(result == FAILED_RESULT){
                return false;
            }else{
                //不需要限流
                return true;
            }
        }catch (Exception e){
            throw new RuntimeException("【流量控制过滤器】redis分布式限流异常", e);
        }
    }
}
