package org.wyh.gateway.core.manager;

import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.register.api.RegisterCenter;
import org.wyh.gateway.register.api.RegisterCenterListener;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.manager
 * @Author: wyh
 * @Date: 2024-05-21 9:52
 * @Description: 注册中心管理类，
                 负责（通过SPI）加载注册中心实例，初始化注册中心，设置监听器和注册网关服务等。
 */
public class RegisterCenterManager {
    //注册中心实例
    private static RegisterCenter registerCenter;
    //注册中心监听器的默认实现
    private static final RegisterCenterListener DEFAULT_LISTENER =
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.manager
     * @Author: wyh
     * @Date: 2024-05-21 10:20
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final RegisterCenterManager INSTANCE = new RegisterCenterManager();
    }
    /**
     * @date: 2024-05-21 10:20
     * @description: private修饰的无参构造器
     * @return: null
     */
    private RegisterCenterManager(){}
    public static RegisterCenterManager getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-21 10:31
     * @description: 负责通过spi加载注册中心实例，调用其初始化方法以及注册网关服务
     * @Param config:
     * @return: void
     */
    public void init(Config config){
        todo
    }
    /**
     * @date: 2024-05-21 10:32
     * @description: 根据传入的注册中心监听器，订阅注册中心的服务变更
     * @Param listener:
     * @return: void
     */
    public void subscribeRegisterCenter(RegisterCenterListener listener){
        todo
    }


}
