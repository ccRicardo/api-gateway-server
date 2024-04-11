package org.wyh.gateway.common.exception;

import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:42
 * @Description: 网关未找到异常
 */
public class GatewayNotFoundException extends GatewayBaseException {
    private static final long serialVersionUID = -5534700534739261761L;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public GatewayNotFoundException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public GatewayNotFoundException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }
}
