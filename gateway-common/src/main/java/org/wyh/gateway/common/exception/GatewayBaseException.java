package org.wyh.gateway.common.exception;

import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:13
 * @Description: 项目的网关异常（发生在网关系统中的异常）基类
 */
public class GatewayBaseException extends RuntimeException{
    //序列化版本号
    private static final long serialVersionUID = -5658789202563433456L;
    //响应状态码
    protected ResponseCode code;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public GatewayBaseException() {
    }

    public GatewayBaseException(String message, ResponseCode code) {
        super(message);
        this.code = code;
    }

    public GatewayBaseException(String message, Throwable cause, ResponseCode code) {
        super(message, cause);
        this.code = code;
    }

    public GatewayBaseException(ResponseCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public GatewayBaseException(String message, Throwable cause,
                                boolean enableSuppression, boolean writableStackTrace, ResponseCode code) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }
    /**
     * @date: 2024-01-17 10:41
     * @description: 获取响应状态码
     * @return: org.wyh.common.enums.ResponseCode
     */
    public ResponseCode getCode() {
        return code;
    }

}
