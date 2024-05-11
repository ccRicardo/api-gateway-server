package org.wyh.gateway.core.request;

import org.asynchttpclient.Request;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.request
 * @Author: wyh
 * @Date: 2024-01-08 14:00
 * @Description: 请求定义接口
 */
public interface IGatewayRequest {
    /**
     * @date: 2024-01-08 14:30
     * @description: 设置目标服务主机地址
     * @Param host: 
     * @return: void
     */
    void setModifyHost(String host);
    /**
     * @date: 2024-01-08 14:35
     * @description: 获得目标服务主机地址
     * @return: java.lang.String
     */
    String getModifyHost();
    /**
     * @date: 2024-01-08 14:35
     * @description: 设置目标服务的路径(http://ip:port后面的路径)
     * @Param path:
     * @return: void
     */
    void setModifyPath(String path);
    /**
     * @date: 2024-01-08 14:37
     * @description: 获得目标服务的路径(http://ip:port后面的路径)
     * @return: java.lang.String
     */
    String getModifyPath();
    /**
     * @date: 2024-01-08 14:40
     * @description: 添加请求头信息（若存在同名header，则追加）
                     CharSequence类型参数可以接收String，StringBuilder和StringBuffer类型的值
     * @Param name:
     * @Param value:
     * @return: void
     */
    void addHeader(CharSequence name, String value);
    /**
     * @date: 2024-01-08 14:43
     * @description: 设置请求头信息（若存在同名header，则覆盖）
     * @Param name:
     * @Param value:
     * @return: void
     */
    void setHeader(CharSequence name, String value);
    /**
     * @date: 2024-01-08 14:45
     * @description: 向GET请求的url中添加参数
     * @Param name:
     * @Param value:
     * @return: void
     */
    void addQueryParam(String name, String value);
    /**
     * @date: 2024-01-08 14:45
     * @description: 向POST请求中的表单中添加参数
     * @Param name:
     * @Param value:
     * @return: void
     */
    void addFormParam(String name, String value);
    /**
     * @date: 2024-01-08 14:48
     * @description: 设置请求超时时间
     * @Param requestTimeout:
     * @return: void
     */
    void setRequestTimeout(int requestTimeout);
    /**
     * @date: 2024-01-08 14:49
     * @description: 获取最终的请求url
     * @return: java.lang.String
     */
    String getFinalUrl();
    /**
     * @date: 2024-01-08 14:49
     * @description: 构造请求对象
                     该项目底层使用了AsyncHttpClient，因此构造的请求对象是org.asynchttpclient.Request类型
     * @return: Request
     */
    Request build();
}
