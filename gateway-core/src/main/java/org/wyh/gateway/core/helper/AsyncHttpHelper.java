package org.wyh.gateway.core.helper;

import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.helper
 * @Author: wyh
 * @Date: 2024-01-17 9:48
 * @Description: 实现异步http请求的辅助类
                 该项目底层使用了AsyncHttpClient框架来实现异步http请求的发送
                 因此该类实际上是对AsyncHttpClient中相关api的封装
 */
public class AsyncHttpHelper {
    //本项目底层通过AsyncHttpClient框架来发送异步http请求
    private AsyncHttpClient asyncHttpClient;
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.helper
     * @Author: wyh
     * @Date: 2024-01-18 9:30
     * @Description: 静态内部类，用于实现单例模式
     */
    private static final class SingletonHolder{
        private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
    }
    /**
     * @date: 2024-01-18 9:33
     * @description: private修饰的构造器
     * @return: null
     */
    private AsyncHttpHelper(){}
    /**
     * @date: 2024-01-18 9:34
     * @description: 获取该类的实例
     * @return: org.wyh.core.helper.AsyncHttpHelper
     */
    public static AsyncHttpHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-01-18 9:38
     * @description: 初始化，设置该类的AsyncHttpClient属性
     * @Param asyncHttpClient:
     * @return: void
     */
    public void initialized(AsyncHttpClient asyncHttpClient){
        this.asyncHttpClient = asyncHttpClient;
    }
    /**
     * @date: 2024-01-18 10:15
     * @description: 使用AsyncHttpClient，发送异步http请求
     * @Param request:
     * @return: java.util.concurrent.CompletableFuture<org.asynchttpclient.Response>
     */
    public CompletableFuture<Response> executeRequest(Request request){
        //ListenableFuture和CompletableFuture都是对java Future接口的拓展，都提供了回调功能
        //至于两者之间的差异，在该项目中可以忽略
        ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
        return future.toCompletableFuture();
    }
    /**
     * @date: 2024-01-18 10:26
     * @description: 使用AsyncHttpClient，发送异步http请求，并且可以自定义AsyncHandler
                     AsyncHandler的主要作用是对异步http请求的响应进行处理
     * @Param request:
     * @Param handler:
     * @return: java.util.concurrent.CompletableFuture<T>
     */
    public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
        //ListenableFuture和CompletableFuture都是对java Future接口的拓展，都提供了回调功能
        //至于两者之间的差异，在该项目中可以忽略
        ListenableFuture<T> future = asyncHttpClient.executeRequest(request, handler);
        return future.toCompletableFuture();
    }
}
