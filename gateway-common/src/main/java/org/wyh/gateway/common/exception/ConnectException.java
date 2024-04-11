package org.wyh.gateway.common.exception;

import lombok.Getter;
import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:47
 * @Description: 连接异常类
 */
public class ConnectException extends BaseException{
    private static final long serialVersionUID = -8503239867913964958L;
    // TODO: 2024-01-17 这个id属性暂时不知道指的是什么
    @Getter
    private final String uniqueId;
    //请求的url
    @Getter
    private final String requestUrl;

    public ConnectException(String uniqueId, String requestUrl) {
        this.uniqueId = uniqueId;
        this.requestUrl = requestUrl;
    }

    public ConnectException(Throwable cause, String uniqueId, String requestUrl, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.uniqueId = uniqueId;
        this.requestUrl = requestUrl;
    }

}
