package org.wyh.gateway.common.exception;

import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:51
 * @Description: 路径不匹配（不存在匹配的路径）异常
 */
public class PathNoMatchedException extends BaseException{
    private static final long serialVersionUID = -6695383751311763169L;

    /*
     * 以下是一系列不同参数的构造函数
     */
    public PathNoMatchedException() {
        this(ResponseCode.PATH_NO_MATCHED);
    }

    public PathNoMatchedException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public PathNoMatchedException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }
}
