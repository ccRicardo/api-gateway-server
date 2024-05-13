package org.wyh.gateway.core.filter.loadbalance;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.exception.NotFoundException;
import org.wyh.gateway.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wyh.gateway.common.enumeration.ResponseCode.SERVICE_INSTANCE_NOT_FOUND;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.loadbalance
 * @Author: wyh
 * @Date: 2024-02-21 15:02
 * @Description: 负载均衡策略-轮询
 */
@Slf4j
public class RoundRobinLoadBalanceRule implements IGatewayLoadBalanceRule{
    //后端服务唯一id
    private final String uniqueId;
    //服务实例集合
    private Set<ServiceInstance> serviceInstanceSet;
    //记录当前轮循到的位置。初始值默认为0。
    private AtomicInteger position = new AtomicInteger();
    /*
     * 保存服务id与对应的RoundRobinLoadBalanceRule对象。
     * 这么做的目的是避免对同一个uniqueId，创建多个重复的RoundRobinLoadBalanceRule对象。
     * 即大幅减少了创建RoundRobinLoadBalanceRule对象的开销。
     */
    private static ConcurrentHashMap<String, RoundRobinLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();
    /**
     * @date: 2024-02-21 15:14
     * @description: 有参构造器，主要负责初始化final修饰的uniqueId属性
     * @Param uniqueId:
     * @return: null
     */
    public RoundRobinLoadBalanceRule(String uniqueId){
        this.uniqueId = uniqueId;
    }
    /**
     * @date: 2024-02-21 15:15
     * @description: 根据uniqueId获取相应的RoundRobinLoadBalanceRule对象。
                     该方法可以避免重复创建RoundRobinLoadBalanceRule对象。
     * @Param uniqueId:
     * @return: org.wyh.core.filter.loadbalance.RoundRobinLoadBalanceRule
     */
    public static RoundRobinLoadBalanceRule getInstance(String uniqueId){
        RoundRobinLoadBalanceRule loadBalanceRule = serviceMap.get(uniqueId);
        if(loadBalanceRule == null){
            loadBalanceRule = new RoundRobinLoadBalanceRule(uniqueId);
            //将该uniqueId的RoundRobinLoadBalanceRule对象存入serviceMap中，供之后使用。
            serviceMap.put(uniqueId, loadBalanceRule);
        }
        return loadBalanceRule;
    }
    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        String uniqueId = ctx.getUniqueId();
        //将具体的业务逻辑委托给另一个重载方法
        return choose(uniqueId, ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String uniqueId, boolean gray) {
        serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(uniqueId, gray);
        if(serviceInstanceSet.isEmpty()){
            log.warn("【负载均衡过滤器】无可用的服务实例: {}", uniqueId);
            throw  new NotFoundException(SERVICE_INSTANCE_NOT_FOUND);
        }
        //将set转换为list，以便能够按照索引取值
        List<ServiceInstance> serviceInstanceList = new ArrayList<>(serviceInstanceSet);
        //获取下一个应该轮询的位置。注意取余。
        int pos = Math.abs(this.position.incrementAndGet()) % serviceInstanceList.size();
        ServiceInstance serviceInstance = serviceInstanceList.get(pos);
        return serviceInstance;
    }
}
