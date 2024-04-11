package org.wyh.gateway.common.utils;

import java.lang.reflect.Method;
import java.util.Properties;
/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-04 10:15
 * @Description: Properties工具类，负责将Properties对象中的属性映射到指定的JavaBean/pojo对象中
 */
public abstract class   PropertiesUtils {
    /**
     * @date: 2024-01-04 10:17
     * @description: 将Properties对象中的属性映射到指定的JavaBean/pojo对象中
     * @Param p:
     * @Param object:
     * @Param prefix: 需要映射的属性的前缀
     * @return: void
     */
    public static void properties2Object(final Properties p, final Object object, String prefix) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            //调用指定对象的所有set方法，完成对应属性的赋值
            if (mn.startsWith("set")) {
                try {
                	//
                    String tmp = mn.substring(4);
                    // 	
                    String first = mn.substring(3, 4);
                    //将方法名中set后面的首字母转小写，然后加上指定前缀，作为key值
                    String key = prefix + first.toLowerCase() + tmp;
                    String property = p.getProperty(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            //以下代码作用：获取参数类型名，然后将String对象转换为相应类型的值
                            String cn = pt[0].getSimpleName();
                            Object arg = null;
                            if (cn.equals("int") || cn.equals("Integer")) {
                                arg = Integer.parseInt(property);
                            } else if (cn.equals("long") || cn.equals("Long")) {
                                arg = Long.parseLong(property);
                            } else if (cn.equals("double") || cn.equals("Double")) {
                                arg = Double.parseDouble(property);
                            } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                                arg = Boolean.parseBoolean(property);
                            } else if (cn.equals("float") || cn.equals("Float")) {
                                arg = Float.parseFloat(property);
                            } else if (cn.equals("String")) {
                                arg = property;
                            } else {
                                continue;
                            }
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }
    
    public static void properties2Object(final Properties p, final Object object) {
        properties2Object(p, object, "");
    }
 
}
