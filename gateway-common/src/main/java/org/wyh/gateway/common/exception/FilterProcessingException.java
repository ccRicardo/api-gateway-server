package org.wyh.gateway.common.exception;

import lombok.Getter;
import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.common.exception
 * @Author: wyh
 * @Date: 2024-05-24 15:12
 * @Description: 过滤器处理异常
 */
public class FilterProcessingException extends BaseException{
    private static final long serialVersionUID = 1L;
    //过滤器id
    @Getter
    private final String filterId;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public FilterProcessingException(String filterId, ResponseCode code){
        super(code.getMessage(), code);
        this.filterId = filterId;
    }
    public FilterProcessingException(Throwable cause, String filterId, ResponseCode code){
        super(code.getMessage(), cause, code);
        this.filterId = filterId;
    }
}
