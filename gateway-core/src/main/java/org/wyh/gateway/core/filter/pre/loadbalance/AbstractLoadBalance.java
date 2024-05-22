package org.wyh.gateway.core.filter.pre.loadbalance;

import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.utils.TimeUtil;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
 * @Author: wyh
 * @Date: 2024-05-22 9:45
 * @Description: 负载均衡抽象类，主要提供了预热权重的计算方法
 */
public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public ServiceInstance select(GatewayContext ctx) {
        //从上下文参数中获取该次请求匹配的服务实例集合（该参数在负载均衡过滤器中设置）
        Set<ServiceInstance> instanceSet = ctx.getAttribute(AttributeKey.MATCHED_INSTANCES);
        if(instanceSet == null || instanceSet.size() == 0){
            return null;
        }
        //将服务实例set转换成对应的服务实例list，方便后续操作
        List<ServiceInstance> instanceList = new ArrayList<>(instanceSet);
        if(instanceList.size() == 1){
            return instanceList.get(0);
        }
        //将负载均衡的具体处理逻辑委托给doSelect方法
        ServiceInstance instance = doSelect(ctx, instanceList);
        //将选中的服务实例放入相应的上下文参数中
        ctx.setAttribute(AttributeKey.SELECTED_INSTANCE, instance);
        return instance;
    }
    /**
     * @date: 2024-05-22 14:27
     * @description: 抽象方法，由具体类实现，负责根据具体的负载均衡策略，从服务实例集合中选出一个实例
     * @Param ctx:
     * @Param instances:
     * @return: org.wyh.gateway.common.config.ServiceInstance
     */
    protected abstract ServiceInstance doSelect(GatewayContext ctx, List<ServiceInstance> instances);
    /**
     * @date: 2024-05-22 15:13
     * @description: 获取服务实例的权重（若该服务实例处于预热期，则需要计算其预热权重）
     * @Param instance:
     * @Param warmUpTime: 预热时间。其值应该大于等于0，若小于0，则使用默认值。
     * @return: int
     */
    protected static int getWeight(ServiceInstance instance){
        int weight;
        //若未设置实例的权重，或者权重值不合理，则使用接口中定义的默认权重
        if(instance.getWeight() == null || instance.getWeight() < 0){
            weight = LoadBalance.DEFAULT_WEIGHT;
        }else{
            weight = instance.getWeight();
        }
        long registerTime = instance.getRegisterTime();
        //获取该服务实例当前的已运行时间（这个值一般用int类型表示就够了）
        int upTime = (int)(TimeUtil.currentTimeMillis() - registerTime);
        //获取预热时间。若给定的预热时间warmUpTime小于0，则使用默认值。
        int warmUp = (instance.getWarmUpTime() < 0 ? LoadBalance.DEFAULT_WARMUP_MS : instance.getWarmUpTime());
        //判断该服务实例是否处于预热期
        if(upTime < warmUp){
            weight = calculateWramUpWeight(upTime, warmUp, weight);
        }
        return weight;

    }
    /**
     * @date: 2024-05-22 14:40
     * @description: 计算预热权重（当服务实例处于预热期内，其预热权重会随运行时间线性增加）
     * @Param upTime: 服务实例已运行时间
     * @Param warmUp: 预热时间
     * @Param weight: 设定的标准权重
     * @return: int
     */
    protected static int calculateWramUpWeight(int upTime, int warmUp, int weight){
        //预热权重与运行时间成正比
        int ww = (int)((float)weight / (float)warmUp * upTime);
        return ww;
    }

}
