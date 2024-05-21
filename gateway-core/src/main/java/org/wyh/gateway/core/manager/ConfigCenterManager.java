package org.wyh.gateway.core.manager;

import org.wyh.gateway.config.api.ConfigCenter;
import org.wyh.gateway.config.api.ConfigCenterListener;
import org.wyh.gateway.config.api.RulesChangeListener;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.register.api.RegisterCenterListener;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.manager
 * @Author: wyh
 * @Date: 2024-05-21 9:56
 * @Description: 配置中心管理类，
                 负责（通过SPI）加载配置中心实例，初始化配置中心和设置监听器等。
 */
public class ConfigCenterManager {
    //配置中心实例
    private static ConfigCenter configCenter;
    //配置中心监听器的默认实现
    private static final ConfigCenterListener DEFAULT_LISTENER =
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.manager
     * @Author: wyh
     * @Date: 2024-05-21 10:10
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final ConfigCenterManager INSTANCE = new ConfigCenterManager();
    }
    /**
     * @date: 2024-05-21 10:09
     * @description: private修饰的无参构造器
     * @return: null
     */
    private ConfigCenterManager(){
    }
    /**
     * @date: 2024-05-21 10:11
     * @description: 返回该类的唯一实例
     * @return: org.wyh.gateway.core.manager.ConfigCenterManager
     */
    public static ConfigCenterManager getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-21 10:13
     * @description: 负责通过spi加载配置中心实例，并调用其初始化方法
     * @Param config:
     * @return: void
     */
    public void init(Config config){
        todo
    }
    /**
     * @date: 2024-05-21 10:18
     * @description: 根据传入的配置中心监听器，订阅配置中心的规则变更
     * @Param listener:
     * @return: void
     */
    public void subscribeConfigCenter(RulesChangeListener listener){
        todo
    }

}
