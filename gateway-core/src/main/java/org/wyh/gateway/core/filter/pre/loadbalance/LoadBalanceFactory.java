package org.wyh.gateway.core.filter.pre.loadbalance;

import java.util.HashMap;
import java.util.Map;

import static org.wyh.gateway.common.constant.FilterConst.LOAD_BALANCE_STRATEGY_RANDOM;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
 * @Author: wyh
 * @Date: 2024-05-22 16:41
 * @Description: 负载均衡工厂类。负责生产（实际上就是获取）指定负载均衡策略的实例。
 */
public class LoadBalanceFactory {
    //保存所有负载均衡策略的实例。其中，key指的是负载均衡策略的名称，value是对应的实例
    private final Map<String, LoadBalance> loadBalanceMap = new HashMap<>();
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
     * @Author: wyh
     * @Date: 2024-05-22 16:50
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final LoadBalanceFactory INSTANCE = new LoadBalanceFactory();
    }
    /**
     * @date: 2024-05-22 16:51
     * @description: private修饰的无参构造器
     * @return: null
     */
    private LoadBalanceFactory(){
        //目前只支持随机负载均衡
        loadBalanceMap.put(LOAD_BALANCE_STRATEGY_RANDOM, new RandomLoadBalance());
    }
    /**
     * @date: 2024-05-22 16:51
     * @description: 获取该类的唯一实例
     * @return: org.wyh.gateway.core.filter.pre.loadbalance.LoadBalanceFactory
     */
    public static LoadBalanceFactory getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-22 16:53
     * @description: 获取指定负载均衡策略的实例
     * @Param loadBalanceStrategy:
     * @return: org.wyh.gateway.core.filter.pre.loadbalance.LoadBalance
     */
    public static LoadBalance getLoadBalance(String loadBalanceStrategy){
        return SingletonHolder.INSTANCE.loadBalanceMap.get(loadBalanceStrategy);
    }

}
