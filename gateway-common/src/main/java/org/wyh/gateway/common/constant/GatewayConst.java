package org.wyh.gateway.common.constant;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.constant
 * @Author: wyh
 * @Date: 2024-01-17 10:55
 * @Description: 网关常量，主要定义网关中常用的参数
 */
public interface GatewayConst {
    String UNIQUE_ID = "uniqueId";

    String DEFAULT_VERSION = "1.0.0";

    String PROTOCOL_KEY = "protocol";

    String META_DATA_KEY = "service_instance";
    int DEFAULT_WEIGHT = 100;
}
