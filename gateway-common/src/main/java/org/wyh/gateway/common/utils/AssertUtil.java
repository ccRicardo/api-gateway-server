package org.wyh.gateway.common.utils;

import org.apache.commons.lang3.StringUtils;
/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-03 15:15
 * @Description: 断言工具类
 */
public class AssertUtil {

    private AssertUtil(){}
    /**
     * @date: 2024-01-03 15:17
     * @description: 判断字符串是否为null,或长度为0
     * @Param string:
     * @Param message: 异常信息
     * @return: void
     */
    public static void notEmpty(String string, String message) {
        if (StringUtils.isEmpty(string)) {
            throw new IllegalArgumentException(message);
        }
    }
    /**
     * @date: 2024-01-03 15:22
     * @description: 判断字符串是否为null,或长度为0,或由空白符组成
     * @Param string:
     * @Param message: 异常信息
     * @return: void
     */
    public static void assertNotBlank(String string, String message) {
        if (StringUtils.isBlank(string)) {
            throw new IllegalArgumentException(message);
        }
    }
    /**
     * @date: 2024-01-03 15:24
     * @description: 判断对象是否为null
     * @Param object:
     * @Param message: 异常信息
     * @return: void
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
    /**
     * @date: 2024-01-03 15:25
     * @description: 判断值是否为true
     * @Param value:
     * @Param message: 异常信息
     * @return: void
     */
    public static void isTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }
    /**
     * @date: 2024-01-03 15:25
     * @description: 判断条件是否为true
     * @Param condition:
     * @Param message: 异常信息
     * @return: void
     */
    public static void assertState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
