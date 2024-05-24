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
    private static final long serialVersionUID = 1L;
    //请求的后台服务的唯一id
    @Getter
    private final String uniqueId;
    //请求的url
    @Getter
    private final String requestUrl;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public ConnectException(String uniqueId, String requestUrl, ResponseCode code) {
        super(code.getMessage(), code);
        this.uniqueId = uniqueId;
        this.requestUrl = requestUrl;
    }
    public ConnectException(Throwable cause, String uniqueId, String requestUrl, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.uniqueId = uniqueId;
        this.requestUrl = requestUrl;
    }

}
