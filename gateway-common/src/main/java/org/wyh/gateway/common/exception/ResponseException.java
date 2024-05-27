package org.wyh.gateway.common.exception;

import lombok.Getter;
import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:53
 * @Description: 响应异常类
 */
public class ResponseException extends BaseException{
    private static final long serialVersionUID = 1L;
    //请求的服务的唯一id
    @Getter
    private String uniqueId;
    /*
     * 以下是一系列不同参数的构造函数
     */

    public ResponseException(String uniqueId, ResponseCode code) {
        super(code.getMessage(), code);
        this.uniqueId = uniqueId;
    }
    public ResponseException(Throwable cause, String uniqueId, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.uniqueId = uniqueId;
    }

}
