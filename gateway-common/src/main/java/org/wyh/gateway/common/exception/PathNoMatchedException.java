package org.wyh.gateway.common.exception;

import lombok.Getter;
import org.wyh.gateway.common.enumeration.ResponseCode;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.exception
 * @Author: wyh
 * @Date: 2024-01-17 10:51
 * @Description: 路径不匹配（不存在匹配的路径）异常
 */
public class PathNoMatchedException extends BaseException{
    private static final long serialVersionUID = 1L;
    //请求的路径
    @Getter
    private String path;
    //请求的服务的唯一id
    @Getter
    private String uniqueId;
    //ANT匹配规则
    @Getter
    private String patternPath;
    /*
     * 以下是一系列不同参数的构造函数
     */
    public PathNoMatchedException(String path, String uniqueId, String patternPath, ResponseCode code) {
        super(code.getMessage(), code);
        this.path = path;
        this.uniqueId = uniqueId;
        this.patternPath = patternPath;
    }

    public PathNoMatchedException(Throwable cause, String path, String uniqueId, String patternPath, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.path = path;
        this.uniqueId = uniqueId;
        this.patternPath = patternPath;
    }
}
