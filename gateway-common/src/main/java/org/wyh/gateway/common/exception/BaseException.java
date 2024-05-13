package org.wyh.gateway.common.exception;

import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:09
 * @Description: 网关异常的基类（不进一步区分网关内部和外部异常）
 */
public class BaseException extends RuntimeException{
    //序列化版本号
    private static final long serialVersionUID = -5658789202563433456L;
    //网关响应状态码
    protected ResponseCode code;

    /*
     * 以下是一系列不同参数的构造函数
     */
    public BaseException() {
    }

    public BaseException(String message, ResponseCode code) {
        super(message);
        this.code = code;
    }

    public BaseException(String message, Throwable cause, ResponseCode code) {
        super(message, cause);
        this.code = code;
    }

    public BaseException(ResponseCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public BaseException(String message, Throwable cause,
                         boolean enableSuppression, boolean writableStackTrace, ResponseCode code) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }
    /**
     * @date: 2024-01-17 10:22
     * @description: 获取网关响应状态码
     * @return: org.wyh.common.enums.ResponseCode
     */
    public ResponseCode getCode() {
        return code;
    }
}
