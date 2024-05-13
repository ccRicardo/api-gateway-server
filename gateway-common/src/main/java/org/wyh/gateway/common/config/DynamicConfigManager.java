package org.wyh.gateway.common.config;

import org.apache.commons.collections.CollectionUtils;
import org.wyh.gateway.common.utils.TimeUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.config
 * @Author: wyh
 * @Date: 2024-01-22 11:00
 * @Description: 动态配置管理类，主要负责缓存和管理动态配置信息，即服务定义，服务实例和规则信息
 * todo：其实这个类应该改名成DynamicInfoManager，也就是动态信息管理器类
 */
public class DynamicConfigManager {
    //缓存服务定义的集合。key为uniqueId，是服务定义的唯一标识
    private ConcurrentHashMap<String, ServiceDefinition> serviceDefinitionMap =
            new ConcurrentHashMap<>();
    //缓存服务实例的集合。key为uniqueId，是服务实例所属的服务定义的唯一标识。一个服务定义与一组服务实例对应。
    private ConcurrentHashMap<String, Set<ServiceInstance>> serviceInstanceMap =
            new ConcurrentHashMap<>();
    //缓存规则的集合。key为ruleId，是规则的唯一标识。
    private ConcurrentHashMap<String, Rule> ruleMap = new ConcurrentHashMap<>();
    //保存路径与规则映射关系的集合。key为路径，value为绑定的规则
    private ConcurrentHashMap<String, Rule> pathRuleMap = new ConcurrentHashMap<>();
    //保存服务名与规则映射关系的集合。key为服务名/id，value为对应的规则集合。
    private ConcurrentHashMap<String, List<Rule>> serviceRuleMap = new ConcurrentHashMap<>();
    /**
     * @date: 2024-01-22 15:20
     * @description: private修饰的无参构造器
     * @return: null
     */
    private DynamicConfigManager() {
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.common.config
     * @Author: wyh
     * @Date: 2024-01-22 15:22
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder {
        private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }

    /**
     * @date: 2024-01-22 15:29
     * @description: 获取该类的单例对象
     * @return: org.wyh.common.config.DynamicConfigManager
     */
    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    /*
     * 对服务定义的相关操作
     */
    /**
     * @date: 2024-01-22 15:31
     * @description: 添加服务定义
     * @Param uniqueId:
     * @Param serviceDefinition:
     * @return: void
     */
    public void putServiceDefinition(String uniqueId,
                                     ServiceDefinition serviceDefinition) {

        serviceDefinitionMap.put(uniqueId, serviceDefinition);;
    }
    /**
     * @date: 2024-01-22 15:32
     * @description: 根据uniqueId获取对应的服务定义
     * @Param uniqueId:
     * @return: org.wyh.common.config.ServiceDefinition
     */
    public ServiceDefinition getServiceDefinition(String uniqueId) {
        return serviceDefinitionMap.get(uniqueId);
    }
    /**
     * @date: 2024-01-22 15:32
     * @description: 根据uniqueId删除对应的服务定义
     * @Param uniqueId:
     * @return: void
     */
    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }
    /**
     * @date: 2024-01-22 15:33
     * @description: 获取整个服务定义集合
     * @return: java.util.concurrent.ConcurrentHashMap<java.lang.String, org.wyh.common.config.ServiceDefinition>
     */
    public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
        return serviceDefinitionMap;
    }
    /*
     * 对服务实例的相关操作
     */
    /**
     * @date: 2024-01-22 15:36
     * @description: 根据uniqueId获取对应的服务实例集合
     * @Param uniqueId:
     * @Param gray: 标识是否要获取灰度服务实例的集合
     * @return: java.util.Set<org.wyh.common.config.ServiceInstance>
     */
    public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId, boolean gray){
        Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
        //判断服务实例集合是否为空
        if(CollectionUtils.isEmpty(serviceInstances)){
            return Collections.emptySet();
        }
        //判断要获取的是灰度服务实例集合还是正常（非灰度）服务实例集合
        if(gray){
            /*
             * 以下代码的作用是从服务实例集合中筛选出灰度服务实例集合
             * stream方法可以将集合转换为（元素）流
             * filter方法的作用是过滤，具体来说，它会遍历流中的每一个元素，并检查表达式结果是否为真。
             * 只有当结果为真，对应元素才会被保留在流中。
             * collect(Collectors.toSet())的作用是将流中的元素收集到一个Set集合中。
             */
            return serviceInstances.stream()
                    .filter(instance->instance.isGray())
                    .collect(Collectors.toSet());
        }
        //从服务实例集合中筛选出正常（非灰度）服务实例集合
        return serviceInstances.stream()
                .filter(instance->!(instance.isGray()))
                .collect(Collectors.toSet());
    }
    /**
     * @date: 2024-01-22 15:36
     * @description: 添加单个服务实例
     * @Param uniqueId:
     * @Param serviceInstance:
     * @return: void
     */
    public void addServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        set.add(serviceInstance);
    }
    /**
     * @date: 2024-01-22 15:38
     * @description: 添加服务实例集合
     * @Param uniqueId:
     * @Param serviceInstanceSet:
     * @return: void
     */
    public void addServiceInstance(String uniqueId, Set<ServiceInstance> serviceInstanceSet) {
        //put方法是覆盖的：如果新插入entry的key在map中已经存在，那么它会覆盖掉旧的entry
        serviceInstanceMap.put(uniqueId, serviceInstanceSet);
    }
    /**
     * @date: 2024-01-22 15:39
     * @description: 更新服务实例
     * @Param uniqueId:
     * @Param serviceInstance:
     * @return: void
     */
    public void updateServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = set.iterator();
        while(it.hasNext()) {
            ServiceInstance is = it.next();
            if(is.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
                //先移除旧值
                it.remove();
                break;
            }
        }
        //移除旧值后，再添加新值
        set.add(serviceInstance);
    }
    /**
     * @date: 2024-01-22 15:43
     * @description: 根据uniqueId和serviceInstanceId删除单个服务实例
     * @Param uniqueId:
     * @Param serviceInstanceId:
     * @return: void
     */
    public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = set.iterator();
        while(it.hasNext()) {
            ServiceInstance is = it.next();
            if(is.getServiceInstanceId().equals(serviceInstanceId)) {
                it.remove();
                break;
            }
        }
    }
    /**
     * @date: 2024-01-22 15:44
     * @description: 根据uniqueId删除服务实例集合
     * @Param uniqueId:
     * @return: void
     */
    public void removeServiceInstancesByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }
    /*
     * 对规则的相关操作
     */
    /**
     * @date: 2024-01-22 16:00
     * @description: 添加单个规则
     * @Param ruleId:
     * @Param rule:
     * @return: void
     */
    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }
    /**
     * @date: 2024-01-22 16:01
     * @description: 加载多个规则（规则列表）。
                     该方法除了要初始化ruleMap外，还要初始化pathRuleMap和serviceRuleMap
     * @Param ruleList:
     * @return: void
     */
    public void putAllRule(List<Rule> ruleList) {
        //该方法除了要初始化ruleMap外，还要初始化pathRuleMap和serviceRuleMap
        ConcurrentHashMap<String,Rule> newRuleMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,Rule> newPathMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,List<Rule>> newServiceMap = new ConcurrentHashMap<>();
        for (Rule rule : ruleList) {
            //更新规则的最后修改时间属性
            rule.setLastModifiedTime(TimeUtil.currentTimeMillis());
            newRuleMap.put(rule.getRuleId(), rule);
            //获取绑定到该rule上的path集合
            List<String> paths = rule.getPaths();
            for (String path : paths) {
                //注意，key中的路径由rule中的服务id和path组合而成
                String key = rule.getServiceId()+"."+path;
                newPathMap.put(key, rule);
            }
            //获取服务对应的规则集合
            List<Rule> rules = newServiceMap.get(rule.getServiceId());
            //判断规则集合是否存在，若不存在，则需要先创建对象。
            if(rules == null){
                rules = new ArrayList<>();
            }
            //将当前的rule对象添加到对应的规则集合中
            rules.add(rule);
            newServiceMap.put(rule.getServiceId(), rules);
        }
        ruleMap = newRuleMap;
        pathRuleMap = newPathMap;
        serviceRuleMap = newServiceMap;
    }
    /**
     * @date: 2024-01-22 16:15
     * @description: 根据ruleId获取指定的规则
     * @Param ruleId:
     * @return: org.wyh.common.config.Rule
     */
    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }
    /**
     * @date: 2024-01-22 16:15
     * @description: 根据ruleId移除指定的规则
     * @Param ruleId:
     * @return: void
     */
    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
    }
    /**
     * @date: 2024-01-22 16:16
     * @description: 获取整个规则集合
     * @return: java.util.concurrent.ConcurrentHashMap<java.lang.String, org.wyh.common.config.Rule>
     */
    public ConcurrentHashMap<String, Rule> getRuleMap() {
        return ruleMap;
    }
    /**
     * @date: 2024-02-20 15:13
     * @description: 获取指定路径绑定的规则
     * @Param path:
     * @return: org.wyh.common.config.Rule
     */
    public Rule getRuleByPath(String path){
        return pathRuleMap.get(path);
    }
    /**
     * @date: 2024-02-20 15:15
     * @description: 获取指定服务对应的规则集合
     * @Param serviceId:
     * @return: java.util.List<org.wyh.common.config.Rule>
     */
    public List<Rule> getRuleByServiceId(String serviceId){
        return serviceRuleMap.get(serviceId);
    }
}
