package org.wyh.gateway.core.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-02-29 10:25
 * @Description: Jedis工具类。Jedis是java连接redis数据库的工具，类似于jdbc和关系型数据库的关系。
                 因此，也可以认为Jedis是redis的java客户端。
                 本工具类提供了基本的redis操作方法，但实际上网关系统只用到了lua脚本相关的两个方法。
 */
// TODO: 2024-02-29 由于目前还未学习redis，对此类的阅读暂未开始
@Slf4j
public class JedisUtil {
    //成功获取分布式锁的标识
    private final String DIST_LOCK_SUCCESS = "OK";
    //成功释放分布式锁的标识
    private final Long DIST_LOCK_RELEASE_SUCCESS = 1L;
    //“key不存在时才进行set操作，否则不做任何操作”的标识
    private final String SET_IF_NOT_EXIST = "NX";
    //“需要设置过期时间”的标识
    private final String SET_WITH_EXPIRE_TIME = "PX";
    //可重入锁
    public static ReentrantLock lock = new ReentrantLock();
    //jedis连接池工具类对象。
    private JedisPoolUtil jedisPool = new JedisPoolUtil();
    /**
     * @date: 2024-03-04 10:58
     * @description: 设置String类型的数据
     * @Param key:
     * @Param value:
     * @return: boolean
     */
    public boolean setString(String key, String value) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.set(key, value);
            return true;
        } catch (Exception e) {
            log.debug("setString() key {} throws:{}", key, e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 13:47
     * @description: 设置String类型的数据，且设置过期时间
     * @Param key:
     * @Param seconds:
     * @Param value:
     * @return: boolean
     */
    public boolean setStringEx(String key, int seconds, String value) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.setex(key, seconds, value);
            return true;
        } catch (Exception e) {
            log.debug("setStringEx() key {} throws:{}",key, e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 13:49
     * @description: 获取String类型的数据
     * @Param key: 
     * @return: java.lang.String
     */
    public String getString(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.get(key);
        } catch (Exception e) {
            log.debug("getString() key {} throws:{}", key,e.getMessage());
            return null;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 13:50
     * @description: 删除String类型的数据
     * @Param key:
     * @return: boolean
     */
    public boolean delString(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.del(key);
            return true;
        } catch (Exception e) {
            log.debug("delString() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:04
     * @description: 删除Hash类型数据中的一个字段
     * @Param key: 数据/键值对的关键字（后同）
     * @Param mKey: 字段关键字/字段名称（后同）
     * @return: boolean
     */
    public boolean delHash(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.hdel(key, mKey);
            return true;
        } catch (Exception e) {
            log.debug("setHash() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:08
     * @description: 设置Hash类型数据的一个字段
     * @Param key:
     * @Param mKey:
     * @Param mVal: 字段值
     * @return: boolean
     */
    public boolean setHash(String key, String mKey, String mVal) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.hset(key, mKey, mVal);
            return true;
        } catch (Exception e) {
            log.debug("setHash() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:09
     * @description: 获取Hash数据的一个字段
     * @Param key:
     * @Param mKey:
     * @return: java.lang.String
     */
    public String getHash(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hget(key, mKey);
        } catch (Exception e) {
            log.debug("setHash() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * @date: 2024-03-04 14:10
     * @description: 设置Hash类型数据的多个字段
     * @Param key:
     * @Param map: 保存字段名与对应的字段值
     * @return: boolean
     */
    public boolean setHashMulti(String key, Map<String, String> map) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.hmset(key, map);
            return true;
        } catch (Exception e) {
            log.debug("setMHash() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:11
     * @description: 获取Hash类型数据的多个字段
     * @Param key:
     * @Param members: 保存要获取的字段名
     * @return: java.util.List<java.lang.String>
     */
    public List<String> getHashMulti(String key, String[] members) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hmget(key, members);
        } catch (Exception e) {
            log.debug("getHashMulti() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * @date: 2024-03-04 14:13
     * @description: 获取Hash类型数据中的所有字段值
     * @Param key:
     * @return: java.util.List<java.lang.String>
     */
    public List<String> getHashValsAll(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hvals(key);
        } catch (Exception e) {
            log.debug("getHashValsAll() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * @date: 2024-03-04 14:20
     * @description: 获取Hash类型数据中的所有字段名
     * @Param key:
     * @return: java.util.Set<java.lang.String>
     */
    public Set<String> getHashKeysAll(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hkeys(key);
        } catch (Exception e) {
            log.debug("getHashValsAll() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * @date: 2024-03-04 14:23
     * @description: 将一个成员及其分数添加到SortedSet类型数据中
     * @Param key: 
     * @Param mKey: 
     * @Param score: 
     * @return: boolean
     */
    public boolean addScoreSet(String key, String mKey, int score) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zadd(key, score, mKey);
            return true;
        } catch (Exception e) {
            log.debug("addScoreSet() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:28
     * @description: 删除SortedSet类型数据中的一个成员
     * @Param key:
     * @Param mKey:
     * @return: boolean
     */
    public boolean delScoreSet(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zrem(key, mKey);
            return true;
        } catch (Exception e) {
            log.debug("delScoreSet() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:30
     * @description: 增加SortedSet类型数据中的一个成员的分数值
     * @Param key: 
     * @Param mKey: 
     * @Param score: 
     * @return: boolean
     */
    public boolean changeScoreSet(String key, String mKey, int score) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zincrby(key, score, mKey);
            return true;
        } catch (Exception e) {
            log.debug("changeScoreSet() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }
    /**
     * @date: 2024-03-04 14:35
     * @description: 按照指定顺序（升序或降序），获取SortedSet类型数据中指定排名/索引范围内的成员
     * @Param key:
     * @Param start:
     * @Param end:
     * @Param asc: 是否为升序
     * @return: java.util.Set<java.lang.String>
     */
    public Set<String> listScoreSetString(String key, int start, int end, boolean asc) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (asc) {
                return jedis.zrange(key, start, end);
            } else {
                return jedis.zrevrange(key, start, end);
            }
        } catch (Exception e) {
            log.debug("listScoreSetString() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * @date: 2024-03-04 14:38
     * @description: 按照指定顺序（升序或降序），获取SortedSet类型数据中指定排名/索引范围内的成员及其分数
     * @Param key:
     * @Param start:
     * @Param end:
     * @Param asc: 是否为升序
     * @return: java.util.Set<redis.clients.jedis.Tuple>
     */
    public Set<Tuple> listScoreSetTuple(String key, int start, int end, boolean asc) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (asc) {
                return jedis.zrangeWithScores(key, start, end);
            } else {
                return jedis.zrevrangeWithScores(key, start, end);
            }
        } catch (Exception e) {
            log.debug("listScoreSetString() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * @date: 2024-03-04 14:47
     * @description: 获取分布式锁
     * @Param lockKey: 
     * @Param requestId: 
     * @Param expireTime: 
     * @return: boolean
     */
    public boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        Jedis jedis = jedisPool.getJedis();
        try {
            /*
             * 该set方法的作用是设置一个String类型的键值对，并且只有当键不存在时才设置，同时设置一个过期时间。
             * 如果设置成功，该方法会返回"OK"。
             * 在这里，这个方法用于设置分布式锁。
             * 其中，值为requestId，这样就可以知道是哪个请求加的锁。
             */
            String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            if (DIST_LOCK_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("getDistributedLock throws {}", e);
        } finally {
            close(jedis);
        }
        return false;
    }
    /**
     * @date: 2024-03-04 14:52
     * @description: 释放分布式锁（通过lua脚本实现，保证了其过程的原子性）
     * @Param lockKey:
     * @Param requestId:
     * @return: boolean
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis jedis = jedisPool.getJedis();
        try {
            /*
             * 该lua脚本的主要作用就是确保只有获取了该锁的线程才能释放该锁，以防止其他线程误解锁，避免了分布式锁的安全问题。
             * 其中，KEYS[1]通常是要释放的锁的名称，ARGV[1]通常是当前线程提供的一个id（通常是请求id），用于做验证。
             * 如果成功释放锁，也就是成功执行redis.call('del', KEYS[1])后，会返回1。
             */
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            if (DIST_LOCK_RELEASE_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("releaseDistributedLock throws {}", e.getMessage());
        } finally {
            close(jedis);
        }
        return false;

    }
    /**
     * @date: 2024-03-04 15:19
     * @description: 归还jedis连接资源。
     * @Param jedis:
     * @return: void
     */
    public void close(Jedis jedis) {
        if (jedis != null) {
            //由于该jedis是从连接池获取的，所以调用close方法实际上执行的操作是归还连接资源，而不是直接关闭连接。
            jedis.close();
        }
    }
    // TODO: 2024-03-04 将以下两个方法的注释补充完整 
    /**
     * @date: 2024-03-04 15:28
     * @description: 执行lua脚本
     * @Param key:
     * @Param limit:
     * @Param expire:
     * @return: java.lang.Object
     */
    public Object executeScript(String key, int limit, int expire){
        Jedis jedis = jedisPool.getJedis();
        String lua = buildLuaScript();
        //将lua脚本加载到redis的脚本缓存中，并返回一个SHA1编码，作为该脚本的唯一标识
        String scriptLoad =jedis.scriptLoad(lua);
        try {
            //evalsha方法可以根据SHA1编码来执行相应的脚本，好处是不需要每次都传输整个脚本，减少了网络传输的数据量。
            Object result = jedis.evalsha(scriptLoad, Arrays.asList(key), Arrays.asList(String.valueOf(expire), String.valueOf(limit)));
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                try {
                    jedis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    
    // 构造lua脚本
    /**
     * @date: 2024-03-04 15:27
     * @description: 构造分布式限流的lua脚本
     * @return: java.lang.String
     */
    private static String buildLuaScript() {
        /*
         * 该lua脚本的主要作用是进行分布式限流，即在设定的时间窗口内，限制某个键（资源对象）的访问次数
         * 具体来说，该脚本可以分为四个部分/步骤：
         * 1、先将KEYS[1]对应的值加1，并存入num（访问次数）中。如果KEYS[1]不存在，redis则会创建该键，并将初值设为0
         * 2、如果num为1，说明是第一次访问，因此给KEYS[1]设定一个过期时间
         * 3、如果num大于ARGV[2]，说明在设定的时间窗口内，访问次数超过了最大限制，那么就进行限流
         * 4、如果以上两个条件都不满足，说明访问次数在允许范围内，因此不做限制
         */
        String lua = "local num = redis.call('incr', KEYS[1])\n" +
                "if tonumber(num) == 1 then\n" +
                "\tredis.call('expire', KEYS[1], ARGV[1])\n" +
                "\treturn 1\n" +
                "elseif tonumber(num) > tonumber(ARGV[2]) then\n" +
                "\treturn 0\n" +
                "else \n" +
                "\treturn 1\n" +
                "end\n";
        return lua;
    }



}
