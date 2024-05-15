package org.wyh.gateway.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.cache
 * @Author: wyh
 * @Date: 2024-03-27 13:42
 * @Description: 基于caffeine的（本地）缓存管理器，用于统一管理系统中用到的各种缓存。
                 （也就是说，系统中的其他部分应该通过该缓存管理器来操作缓存，而不是直接操作）
                 实际上，目前只有过滤器链部分使用到了缓存。
 */
public class GatewayCacheManager {
    //保存系统中需要用到的各种缓存。其中key是缓存的名称/id，value是保存相关数据的缓存实例。
    private final ConcurrentHashMap<String, Cache<String, ?>> cacheMap = new ConcurrentHashMap<>();
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.cache
     * @Author: wyh
     * @Date: 2024-03-27 13:53
     * @Description: 静态内部类，用于创建并保存该类的唯一实例，实现单例模式
     */
    private static class SingletonInstance {
        //该类的唯一实例
        private static final GatewayCacheManager INSTANCE = new GatewayCacheManager();
    }
    /**
     * @date: 2024-03-27 13:52
     * @description: private修饰的无参构造器，用于实现单例模式
     * @return: null
     */
    private GatewayCacheManager(){
    }
    /**
     * @date: 2024-03-27 13:55
     * @description: 获取该类的唯一实例
     * @return: org.wyh.core.cache.GatewayCacheManager
     */
    public static GatewayCacheManager getInstance(){
        return SingletonInstance.INSTANCE;
    }
    /**
     * @date: 2024-03-27 14:08
     * @description: 根据默认配置参数，创建指定缓存名称对应的缓存实例。
                     其中，泛型V指缓存中数据的类型，后面的方法同理。
     * @Param cacheId:
     * @return: com.github.benmanes.caffeine.cache.Cache<java.lang.String, V>
     */
    public <V> Cache<String, V> create(String cacheId){
        //根据默认配置参数，创建Caffeine缓存实例
        Cache<String, V> cache = Caffeine.newBuilder().build();
        cacheMap.put(cacheId, cache);
        return (Cache<String, V>)cacheMap.get(cacheId);
    }
    /**
     * @date: 2024-03-27 14:29
     * @description: 清空指定缓存名称对应的缓存实例。
     * @Param cacheId:
     * @return: void
     */
    public <V> void remove(String cacheId){
        Cache<String, V> cache = (Cache<String, V>)cacheMap.get(cacheId);
        if(cache != null){
            //实际上，invalidateAll会将缓存中的所有项都标记为无效，而不是立即删除。
            cache.invalidateAll();
        }
    }
    /**
     * @date: 2024-03-27 14:43
     * @description:
     * @Param cacheId: 清除指定缓存中的指定缓存项。
     * @Param key:
     * @return: void
     */
    public <V> void remove(String cacheId, String key){
        Cache<String, V> cache = (Cache<String, V>)cacheMap.get(cacheId);
        if(cache != null){
            //invalidate会将指定key对应的缓存项标记为无效，而不是立即删除。
            cache.invalidate(key);
        }
    }
    /**
     * @date: 2024-03-27 14:34
     * @description: 清空所有的缓存
     * @return: void
     */
    public <V> void removeAll(){
        cacheMap.values().forEach(cache -> cache.invalidateAll());
    }
}
