package org.wyh.gateway.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-05-13 13:35
 * @Description: 集合工具类，负责集合相关的操作
 */
public class CollectionUtils {
    /**
     * @date: 2024-05-13 13:45
     * @description: 私有无参构造器。工具类的方法通常都是静态方法，不需要构建类对象。
     * @return: null
     */
    private CollectionUtils() {
    }
    /**
     * @date: 2024-05-13 13:52
     * @description: 判断两个map集合是否相同（即包含的元素是否完全相同）
     * @Param map1:
     * @Param map2:
     * @return: boolean
     */
    public static boolean mapEquals(Map<?, ?> map1, Map<?, ?> map2) {
        if (map1 == null && map2 == null) {
            return true;
        }
        if (map1 == null || map2 == null) {
            return false;
        }
        if (map1.size() != map2.size()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            Object key = entry.getKey();
            Object value1 = entry.getValue();
            Object value2 = map2.get(key);
            if (!objectEquals(value1, value2)) {
                return false;
            }
        }
        return true;
    }
    /**
     * @date: 2024-05-13 13:54
     * @description: 私有方法，判断两个对象是否相等
     * @Param obj1:
     * @Param obj2:
     * @return: boolean
     */
    private static boolean objectEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        //具体的相等标准取决于obj1和obj2的具体类型。如果该类型没有重写equals方法，则使用Object.equals。
        return obj1.equals(obj2);
    }
    /**
     * @date: 2024-05-13 13:58
     * @description: 将一个字符串数组形式的kv对序列转换为相应的map集合
     * @Param pairs:
     * @return: java.util.Map<java.lang.String, java.lang.String>
     */
    public static Map<String, String> toStringMap(String... pairs) {
        Map<String, String> parameters = new HashMap<>();
        if (pairs.length > 0) {
            //验证字符串数组的元素数量是否为偶数
            if (pairs.length % 2 != 0) {
                throw new IllegalArgumentException("pairs must be even.");
            }
            for (int i = 0; i < pairs.length; i = i + 2) {
                parameters.put(pairs[i], pairs[i + 1]);
            }
        }
        return parameters;
    }

    /**
     * @date: 2024-05-13 14:02
     * @description: 将一个对象数组形式的kv对序列转换为相应的map集合
     * @Param pairs:
     * @return: java.util.Map<K, V>
     */
    public static <K, V> Map<K, V> toMap(Object... pairs) {
        Map<K, V> ret = new HashMap<>();
        if (pairs == null || pairs.length == 0) {
            return ret;
        }
        //验证对象数组的元素数量是否为偶数
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs can not be odd number.");
        }
        int len = pairs.length / 2;
        for (int i = 0; i < len; i++) {
            ret.put((K) pairs[2 * i], (V) pairs[2 * i + 1]);
        }
        return ret;
    }
    /**
     * @date: 2024-05-13 14:04
     * @description: 判断Collection集合是否为空（集合对象为null或集合不包含任何元素）
     * @Param collection:
     * @return: boolean
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    /**
     * @date: 2024-05-13 14:05
     * @description: 判断Collection集合是否为非空（集合对象不为null且包含至少一个元素）
     * @Param collection:
     * @return: boolean
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    /**
     * @date: 2024-05-13 14:05
     * @description: 判断一个map集合是否为空
     * @Param map:
     * @return: boolean
     */
    public static boolean isEmptyMap(Map map) {
        return map == null || map.size() == 0;
    }
    /**
     * @date: 2024-05-13 14:06
     * @description: 判断一个map集合是否为非空
     * @Param map:
     * @return: boolean
     */
    public static boolean isNotEmptyMap(Map map) {
        return !isEmptyMap(map);
    }

}