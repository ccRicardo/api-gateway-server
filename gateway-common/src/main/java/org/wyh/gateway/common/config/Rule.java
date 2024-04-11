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
                 由于规则匹配时需要比较规则优先级，所以需要实现Comparable<Rule>接口

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
    //规则优先级。应用场景为一条路径对应多条规则时。
    private Integer order;
    //规则对应的后端服务id。通常来说，一个后端服务可以对应多个规则。
    private String serviceId;
    //规则对应的请求前缀。todo 该属性的作用目前还不大明白，猜测是用来标识要请求的后端服务的类别。
    private String prefix;
    //规则绑定的路径集合。通常来说，一个规则可以绑定多条请求路径。
    private List<String> paths;
    //过滤器配置集合（也就是一条过滤器链）。
    private Set<FilterConfig> filterConfigs = new HashSet<>();
    //限流配置集合
    private Set<FlowCtrlConfig> flowCtrlConfigs = new HashSet<>();
    //熔断配置集合
    private Set<HystrixConfig> hystrixConfigs = new HashSet<>();
    //请求重试的配置信息
    private RetryConfig retryConfig = new RetryConfig();
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
        //该过滤器的配置信息
        private String Config;

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
                    ", Config='" + Config + '\'' +
                    '}';
        }
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.common.config
     * @Author: wyh
     * @Date: 2024-02-26 14:08
     * @Description: 内部类，用于定义请求重试机制的配置信息
     */
    @Setter
    @Getter
    public static class RetryConfig{
        //请求重试的次数
        private int times;
        // TODO: 2024-02-26 还可以考虑添加更多的属性 
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.common.config
     * @Author: wyh
     * @Date: 2024-02-27 10:19
     * @Description: 内部类，用于定义流量控制的配置信息
     */
    @Setter
    @Getter
    public static class FlowCtrlConfig{
        //限流的类型，可以是路径，服务id或者ip
        private String type;
        //限流的对象（值）
        private String value;
        //限流的模式，单机或分布式
        private String mode;
        //限流的规则配置，以json串的形式给出
        private String config;
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.common.config
     * @Author: wyh
     * @Date: 2024-03-05 14:47
     * @Description: 内部类，用于定义熔断降级相关的配置信息
     */
    @Setter
    @Getter
    public static class HystrixConfig{
        //需要加断路器的请求路径
        private String path;
        //请求的超时时间（准确来说，是HystrixCommand中run方法的超时时间），以ms为单位
        private int timeoutInMilliseconds;
        //线程池的核心线程数
        private int threadCoreSize;
        //降级回退时的响应内容
        private String fallbackResponse;
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
     * @description: 判断filterId指定的过滤器配置是否存在
     * @Param filterId:
     * @return: boolean
     */
    public boolean checkConfigExists(String filterId){
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
                ", serviceId='" + serviceId + '\'' +
                ", prefix='" + prefix + '\'' +
                ", paths=" + paths +
                ", filterConfigs=" + filterConfigs +
                ", flowCtrlConfigs=" + flowCtrlConfigs +
                ", hystrixConfigs=" + hystrixConfigs +
                ", retryConfig=" + retryConfig +
                '}';
    }
}
