package org.wyh.gateway.common.constant;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.constant
 * @Author: wyh
 * @Date: 2024-01-17 10:57
 * @Description: 网关协议常量，主要定义协议相关的常量
 */
public interface GatewayProtocol {
    String HTTP = "http";

    String DUBBO = "dubbo";
    /**
     * @date: 2024-01-17 10:58
     * @description: 判断是否使用http协议
     * @Param protocol:
     * @return: boolean
     */
    static boolean isHttp(String protocol) {
        return HTTP.equals(protocol);
    }
    /**
     * @date: 2024-01-17 10:58
     * @description: 判断是否使用dubbo协议
     * @Param protocol:
     * @return: boolean
     */
    static boolean isDubbo(String protocol) {
        return DUBBO.equals(protocol);
    }
}
