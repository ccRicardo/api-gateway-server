package org.wyh.gateway.core.manager;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.config.api.ConfigCenter;
import org.wyh.gateway.config.api.ConfigCenterListener;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.register.api.RegisterCenterListener;

import java.util.ServiceLoader;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.manager
 * @Author: wyh
 * @Date: 2024-05-21 9:56
 * @Description: 配置中心管理类，
                 负责（通过SPI）加载配置中心实例，初始化配置中心和设置监听器等。
 */
@Slf4j
public class ConfigCenterManager {
    //配置中心实例
    private static ConfigCenter configCenter;
    //配置中心监听器的默认实现
    public static final ConfigCenterListener DEFAULT_LISTENER = new ConfigCenterListener() {
        @Override
        public void onRulesChange(Rule rule) {
            DynamicConfigManager manager = DynamicConfigManager.getInstance();
            //将更新后的规则缓存到本地
            manager.putRule(rule.getRuleId(), rule);
        }
    };
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
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建配置中心实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * ServiceLoader.findFirst方法返回的是第一个（种）实现类的实例。
         */
        ServiceLoader<ConfigCenter> configCenters = ServiceLoader.load(ConfigCenter.class);
        configCenter = configCenters.findFirst().orElseThrow(() -> {
            //如果没找到实现类，则执行以下lambda表达式，抛出异常
            log.error("未找到配置中心实例");
            return new RuntimeException("未找到配置中心实例");
        });
        //初始化
        configCenter.init(config.getConfigAddress(), config.getEnv());
    }
    /**
     * @date: 2024-05-21 10:18
     * @description: 根据传入的配置中心监听器，订阅配置中心的规则变更
     * @Param listener:
     * @return: void
     */
    public void subscribeConfigCenter(ConfigCenterListener listener){
        configCenter.subscribeRulesChange(listener);
    }

}
