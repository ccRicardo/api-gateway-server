package org.wyh.gateway.core.config;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.utils.PropertiesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core
 * @Author: wyh
 * @Date: 2024-01-15 10:09
 * @Description: 网关配置加载类，支持多种配置加载方式。
 */
// TODO: 2024-05-17 更改了环境变量的前缀，k8s配置文件还未更改 
@Slf4j
public class ConfigLoader {
    //配置文件名称
    private static final String CONFIG_FILE = "gateway.properties";
    //环境变量的前缀
    private static final String ENV_PREFIX = "gateway_";
    //jvm参数的前缀
    private static final String JVM_PREFIX = "gateway.";
    //配置类Config对象。注意这里并没有创建相应的实例，而是在load方法中才创建相应实例。
    private Config config;
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core
     * @Author: wyh
     * @Date: 2024-05-17 14:46
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final ConfigLoader INSTANCE = new ConfigLoader();
    }
    /**
     * @date: 2024-01-15 10:36
     * @description: private修饰的无参构造器
     * @return: null
     */
    private ConfigLoader(){}
    /**
     * @date: 2024-01-15 10:48
     * @description: 获取ClassLoader唯一实例
     * @return: org.wyh.core.ConfigLoader
     */
    public static ConfigLoader getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-01-15 10:38
     * @description: 获取单例对象INSTANCE的config属性
     * @return: org.wyh.core.Config
     */
    public static Config getConfig(){
        return SingletonHolder.INSTANCE.config;
    }
    /**
     * @date: 2024-01-15 10:40
     * @description: 加载配置
                     此方法支持多种配置方式，配置方式的优先级如下：
                     运行参数 > jvm参数 > 环境变量 > 配置文件 > 配置对象默认值
     * @Param args:
     * @return: org.wyh.core.Config
     */
    public Config load(String args[]){
        //使用配置对象默认值
        config = new Config();
        //从配置文件加载配置
        loadFromConfigFile();
        //从环境变量中获取配置
        loadFromEnv();
        //从jvm参数中获取配置
        loadFromJvm();
        //从运行参数中获取配置
        loadFromArgs(args);
        return config;
    }
    /**
     * @date: 2024-01-15 10:51
     * @description: 从配置文件加载配置
     * @return: void
     */
    private void loadFromConfigFile(){

        InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if(is != null){
            Properties properties = new Properties();
            try{
                //将properties文件加载到properties对象中
                properties.load(is);
                //将properties对象中的属性映射到Config对象的相应字段上
                PropertiesUtils.properties2Object(properties, config);

            }catch (IOException e){
                //打印异常日志
                log.warn("无法加载配置文件: {}", CONFIG_FILE, e);
            }finally {
                if(is != null){
                    try{
                        //关闭流
                        is.close();
                    }catch (IOException e){
                        //暂时不做处理
                    }
                }
            }
        }
    }
    /**
     * @date: 2024-01-15 10:52
     * @description: 从环境变量中获取配置
     * @return: void
     */
    private void loadFromEnv(){
        //获取所有的环境变量
        Map<String, String> env = System.getenv();
        Properties properties = new Properties();
        properties.putAll(env);
        //将properties对象中的属性映射到Config对象的相应字段上
        PropertiesUtils.properties2Object(properties, config, ENV_PREFIX);

    }
    /**
     * @date: 2024-01-15 10:52
     * @description: 从jvm参数中获取配置
     * @return: void
     */
    private void loadFromJvm(){
        //获取系统属性（jvm参数）
        Properties properties = System.getProperties();
        //将properties对象中的属性映射到Config对象中
        PropertiesUtils.properties2Object(properties, config, JVM_PREFIX);
    }
    /**
     * @date: 2024-01-15 10:53
     * @description: 从运行参数中获取配置
     * @Param args: 运行参数，格式："--port=1234"
     * @return: void
     */
    private void loadFromArgs(String args[]){
        if(args != null && args.length > 0){
            Properties properties = new Properties();
            for (String arg : args) {
                //运行参数格式：形如"--port=1234"
                if(arg.startsWith("--") && arg.contains("=")){
                    //将参数名和参数值提取出来，放到map中
                    properties.put(arg.substring(2, arg.indexOf("=")),
                            arg.substring(arg.indexOf("=")+1));
                }
            }
            PropertiesUtils.properties2Object(properties, config);
        }
    }

}
