package org.wyh.gateway.common.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.config
 * @Author: wyh
 * @Date: 2024-01-17 13:49
 * @Description: 服务定义类，用于描述服务的信息（下游服务都需要注册到注册中心）
                 “@Builder”注解的作用是实现建造者模式
 */
@Setter
@Getter
@Builder
public class ServiceDefinition implements Serializable {
    private static final long serialVersionUID = -8263365765897285189L;
    //服务唯一id，由服务id和版本号构成，形如：serviceId:version
    private String uniqueId;
    //服务id
    private String serviceId;
    //服务版本号
    private String version;
    //服务采用的具体协议（也就是RPC采用的具体协议），例如http(mvc http), dubbo等
    private String protocol;
    //服务的路径匹配规则，是一个ANT风格的表达式（正则匹配的效率太低，所以使用ANT）
    // TODO: 2024-04-01 目前尚未使用该属性，计划在发送请求前进行一次路径匹配
    private String patternPath;
    //环境类型，例如开发环境，测试环境等
    private String envType;
    //服务启用或禁用
    private boolean enable = true;
    //服务的描述信息
    private String desc;
    //服务的方法调用集合。一个服务可以向外暴露多个调用方法。
    //Map中的key表示方法调用的路径，即url中ip:port后面的路径部分
    private Map<String, ServiceInvoker> invokerMap;
    /**
     * @date: 2024-01-17 14:30
     * @description: 无参构造器
     * @return: null
     */
    public ServiceDefinition() {
        super();
    }
    /**
     * @date: 2024-01-17 14:30
     * @description: 有参构造器
     * @Param uniqueId:
     * @Param serviceId:
     * @Param version:
     * @Param protocol:
     * @Param patternPath:
     * @Param envType:
     * @Param enable:
     * @Param invokerMap:
     * @return: null
     */
    public ServiceDefinition(String uniqueId, String serviceId, String version, String protocol, String patternPath,
                             String envType, boolean enable, String desc, Map<String, ServiceInvoker> invokerMap) {
        super();
        this.uniqueId = uniqueId;
        this.serviceId = serviceId;
        this.version = version;
        this.protocol = protocol;
        this.patternPath = patternPath;
        this.envType = envType;
        this.enable = enable;
        this.desc = desc;
        this.invokerMap = invokerMap;
    }
    @Override
    public boolean equals(Object o) {
        if(this == o){
            return true;
        }
        if(this == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceDefinition serviceDefinition = (ServiceDefinition)o;
        //通过uniqueId属性判断两个ServiceDefinition对象是否相同
        return Objects.equals(uniqueId, serviceDefinition.uniqueId);
    }

    @Override
    public int hashCode() {
        //根据uniqueId生成hash码
        return Objects.hash(uniqueId);
    }

    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "uniqueId='" + uniqueId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", version='" + version + '\'' +
                ", protocol='" + protocol + '\'' +
                ", patternPath='" + patternPath + '\'' +
                ", envType='" + envType + '\'' +
                ", enable=" + enable +
                ", desc='" + desc + '\'' +
                ", invokerMap=" + invokerMap +
                '}';
    }
}
