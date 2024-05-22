package org.wyh.gateway.core.filter.pre.loadbalance;

import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
 * @Author: wyh
 * @Date: 2024-05-22 15:25
 * @Description: （带权）随机负载均衡。服务实例的权重越高，被选中的概率越高。
 */
public class RandomLoadBalance extends AbstractLoadBalance{
    @Override
    protected ServiceInstance doSelect(GatewayContext ctx, List<ServiceInstance> instances) {
        int size = instances.size();
        //总权重
        int totalWeight = 0;
        //标识是否每个实例的权重都相同
        boolean sameWeight = true;
        for(int i = 0; i < size; i++){
            int weight = super.getWeight(instances.get(i));
            totalWeight += weight;
            //判断当前实例与上一个实例的权重值是否相同。（若sameWeight已经为false，则没有比较的必要）
            if(sameWeight && i > 0 && weight != super.getWeight(instances.get(i-1))){
                sameWeight = false;
            }
        }
        //若实例的权重不全都相同，则根据权重进行随机选择（带权随机）
        if(!sameWeight){
            /*
             * 带权随机算法的描述：
             * 1、将实例列表中的实例依次放到一个一维坐标系上，并将权重值依次累加，以作为这些实例的坐标值
             * 2、根据总权重值随机出一个偏移量（随机范围为[0,totalWeight)）
             * 3、在坐标系上，找到第一个坐标值大于该偏移量的实例，该实例就是要选择的实例
             * 注：ThreadLocalRandom.current().nextInt方法的随机范围也是左闭右开
             */
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (ServiceInstance instance : instances) {
                offset -= getWeight(instance);
                if(offset < 0){
                    return instance;
                }
            }
        }
        //若实例的权重全都相同，则随机选择一个实例即可
        return instances.get(ThreadLocalRandom.current().nextInt(size));
    }
}
