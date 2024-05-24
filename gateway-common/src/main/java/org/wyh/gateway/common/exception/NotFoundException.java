package org.wyh.gateway.common.exception;

import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:46
 * @Description: 未找到异常类
 */
public class NotFoundException extends BaseException{
    private static final long serialVersionUID = 1L;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public NotFoundException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public NotFoundException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }
}
