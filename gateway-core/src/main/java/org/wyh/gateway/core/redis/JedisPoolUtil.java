package org.wyh.gateway.core.redis;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.config.ConfigLoader;
import redis.clients.jedis.*;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-02-29 10:25
 * @Description: JedisPool工具类。JedisPool是一个线程安全的Jedis连接池，负责管理Jedis连接对象。
 */
@Slf4j
public class JedisPoolUtil {
    //JedisPool实例，通过redis.properties配置文件完成相关参数设置。
    public static JedisPool jedisPool = null;
    //redis主机地址
    private String host;
    //redis服务的端口号
    private int port;
    //连接池的最大连接数
    private int maxTotal;
    //连接池的最大空闲连接数
    private int maxIdle;
    //连接池的最小空闲连接数
    private int minIdle;
    //标识当连接池耗尽时，是否阻塞等待
    private boolean blockWhenExhausted;
    //最长等待时间
    private int maxWaitMillis;
    //标识是否在获取连接时进行有效性检查（也就是健康检查）
    private boolean testOnBorrow;
    //标识是否在归还连接时进行有效性检查（也就是健康检查）
    private boolean testOnReturn;
    //可重入锁，主要是对连接池初始化过程加锁，以保证其多线程安全。
    public static Lock lock = new ReentrantLock();
    /**
     * @date: 2024-03-04 10:13
     * @description: 初始化jedis连接池的配置
     * @return: void
     */
    private void initialConfig() {
        try {
            //获取配置信息
            Config config = ConfigLoader.getConfig();
            host = config.getRedisHost();
            port = config.getRedisPort();
            maxTotal = config.getRedisMaxTotal();
            maxIdle = config.getRedisMaxIdle();
            minIdle = config.getRedisMinIdle();
            //以下参数暂时未配置
            /*blockWhenExhausted;
            maxWaitMillis;
            testOnBorrow;
            testOnReturn;*/
        } catch (Exception e) {
            log.debug("parse configure file error.");
        }
    }

    /**
     * @date: 2024-03-04 10:15
     * @description: 初始化jedis连接池对象
     * @return: void
     */
    private void initialPool() {
        //尝试获取锁。若获取成功，则进行连接池对象初始化操作，否则阻塞1秒。
        if (lock.tryLock()) {
            //加锁，保证初始化过程是多线程安全的。
            lock.lock();
            initialConfig();
            try {
                //创建连接池配置类对象，并设置相应的配置参数。
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(maxTotal);
                config.setMaxIdle(maxIdle);
                config.setMaxWaitMillis(maxWaitMillis);
                config.setTestOnBorrow(testOnBorrow);
                jedisPool = new JedisPool(config, host, port);
            } catch (Exception e) {
                log.debug("init redis pool failed : {}", e.getMessage());
            } finally {
                //释放锁。
                lock.unlock();
            }
        } else {
            log.debug("some other is init pool, just wait 1 second.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    /**
     * @date: 2024-03-04 10:37
     * @description: 获取Jedis连接对象
     * @return: redis.clients.jedis.Jedis
     */
    public Jedis getJedis() {

        if (jedisPool == null) {
            initialPool();
        }
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            log.debug("getJedis() throws : {}" + e.getMessage());
        }
        return null;
    }
    /**
     * @date: 2024-03-04 10:38
     * @description: 返回Pipeline对象，用于执行redis命令的批量操作。
     * @return: redis.clients.jedis.Pipeline
     */
    public Pipeline getPipeline() {
        //BinaryJedis提供了以字节流形式操作Redis的方式。
        BinaryJedis binaryJedis = new BinaryJedis(host, port);
        /*
         * pipelined方法的作用是创建一个新的Pipeline对象。Pipeline一般用于执行批量操作。
         * 具体来说，就是一次发送多个命令到redis服务器，然后一次性读取所有的响应。
         * 这种方式减少了网络往返的次数，因此可以显著地提高效率。
         */
        return binaryJedis.pipelined();
    }
}
