package org.wyh.gateway.common.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.config
 * @Author: wyh
 * @Date: 2024-01-08 9:49
 * @Description: 可插拔式过滤规则定义类（可插拔主要体现为按需取用）
                 规则实际上就是描述了某一服务或路径需要使用哪些过滤组件，组件的执行顺序以及各组件的配置参数等。
                 由于一条路径可以绑定多个规则，而最终应用时需要进行优先级比较来选择一个具体规则，
                 所以该规则类需要实现Comparable<Rule>接口

 */
@Setter
@Getter
public class Rule implements Comparable<Rule>, Serializable{
    //规则id
    private String ruleId;
    //规则的最后修改时间（以保存到动态配置管理器时的时间为准）
    private long lastModifiedTime;
    //规则名称
    private String name;
    //规则对应的协议
    private String protocol;
    //规则优先级。一条路径可以绑定多个规则，而最终应用时需要进行优先级比较来选择一个具体规则。优先级数字越小，执行的顺序越靠前。
    private Integer order;
    //规则的过滤器配置集合（其实就是在定义一条过滤器链）。
    private Set<FilterConfig> filterConfigs = new HashSet<>();
    /**
     * @date: 2024-01-11 14:04
     * @description: 无参构造器
     * @return: null
     */
    public Rule(){
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.common.config
     * @Author: wyh
     * @Date: 2024-01-11 14:09
     * @Description: 内部类，用于定义过滤器配置信息
     */
    @Setter
    @Getter
    public static class FilterConfig{
        //过滤器的id
        private String filterId;
        //该过滤器的配置信息，通常是一个json串
        private String config;

        @Override
        public int hashCode() {
            //根据filterId字段生成hashcode
            return Objects.hash(filterId);
        }

        @Override
        public boolean equals(Object obj) {
            //判断是否是同一个对象
            if(this == obj){
                return true;
            }
            //判断类型是否相同
            if(obj == null || this.getClass() != obj.getClass()){
                return false;
            }
            FilterConfig that = (FilterConfig) obj;
            //通过filterId判断是否为同一过滤器的配置。
            return this.filterId.equals(that.filterId);
        }

        @Override
        public String toString() {
            return "FilterConfig{" +
                    "filterId='" + filterId + '\'' +
                    ", Config='" + config + '\'' +
                    '}';
        }
    }
    /**
     * @date: 2024-01-11 14:38
     * @description: 添加过滤器配置
     * @Param filterConfig:
     * @return: boolean
     */
    public boolean addFilterConfig(FilterConfig filterConfig){
        return filterConfigs.add(filterConfig);
    }
    /**
     * @date: 2024-01-11 14:40
     * @description: 根据filterId获取相应的过滤器的配置信息
     * @Param filterId:
     * @return: org.wyh.common.config.Rule.FilterConfig
     */
    public FilterConfig getFilterConfig(String filterId){
        for(FilterConfig filterConfig : filterConfigs){
            if(filterConfig.getFilterId().equalsIgnoreCase(filterId)){
                return filterConfig;
            }
        }
        return null;
    }
    /**
     * @date: 2024-01-11 14:53
     * @description: 检查给定filterId对应的过滤器组件是否存在/启用
     * @Param filterId:
     * @return: boolean
     */
    public boolean checkFilterExist(String filterId){
        for (FilterConfig filterConfig : filterConfigs) {
            if(filterConfig.getFilterId().equalsIgnoreCase(filterId)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Rule o) {
        //先比较规则的优先级。优先级相同，再按字典序比较规则id
        int orderCompare = Integer.compare(this.getOrder(), o.getOrder());
        if(orderCompare == 0){
            return this.getRuleId().compareTo(o.getRuleId());
        }
        return orderCompare;
    }

    @Override
    public int hashCode() {
        //根据ruleId字段生成hashcode
        return Objects.hash(ruleId);
    }

    @Override
    public boolean equals(Object obj) {
        //判断是否是同一个对象
        if(this == obj){
            return true;
        }
        //判断类型是否相同
        if(obj == null || this.getClass() != obj.getClass()){
            return false;
        }
        Rule that = (Rule) obj;
        //通过ruleId判断是否为同一规则。
        return this.ruleId.equals(that.ruleId);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "ruleId='" + ruleId + '\'' +
                ", lastModifiedTime=" + lastModifiedTime +
                ", name='" + name + '\'' +
                ", protocol='" + protocol + '\'' +
                ", order=" + order +
                ", filterConfigs=" + filterConfigs +
                '}';
    }
}
