package org.wyh.gateway.common.exception;

import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:53
 * @Description: 响应异常类
 */
public class ResponseException extends BaseException{
    private static final long serialVersionUID = -5658789202509039759L;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public ResponseException() {
        this(ResponseCode.INTERNAL_ERROR);
    }
    public ResponseException(ResponseCode code) {
        super(code.getMessage(), code);
    }
    public ResponseException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.code = code;
    }

}
