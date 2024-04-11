package org.wyh.gateway.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.config
 * @Author: wyh
 * @Date: 2024-01-22 10:46
 * @Description: dubbo服务方法调用的具体实现类
                 方法调用指的是服务向外暴露/提供的可调用方法
 */
@Setter
@Getter
public class DubboServiceInvoker extends AbstractServiceInvoker{
    //注册中心地址
    private String registerAddress;
    //服务接口的全类名（dubbo可以通过服务接口的全类名来获取对应的代理对象（实现类对象））
    private String interfaceClass;
    //方法的名称
    private String methodName;
    //方法的参数类型（类型名用全类名）。（java中方法签名由方法名称和参数类型共同决定）
    private String[] parameterTypes;
    //dubbo服务的版本号
    private String version;
}
