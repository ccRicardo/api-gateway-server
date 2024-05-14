package org.wyh.gateway.core.context;

import org.wyh.gateway.common.config.ServiceInvoker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.context
 * @Author: wyh
 * @Date: 2024-05-13 16:59
 * @Description: 本抽象类的作用就是表示/代表上下文参数的key。其实现类是它的一个静态内部类。
                 Q: 为什么不直接使用String作为上下文参数（以下简称为参数）key的类型
                 A: 若直接使用String作为参数key的类型，则参数value的类型只能是Object
                    这会导致在设置和获取参数value时不方便（设置时无法验证value的具体类型，获取时也无法得知value的具体类型）
                    而单独定义一个类表示参数key，则可以通过使用泛型来指定/规定参数value的具体类型
 */
public abstract class AttributeKey<T> {
    //保存预定义好（即已经命名好）的AttributeKey对象。其中，key是AttributeKey的名称。
    private static Map<String, AttributeKey<?>> namedMap = new HashMap<>();
    //（预定义好的）表示ip地址列表（上下文）参数key的AttributeKey对象
    public static final AttributeKey<Set<String>> IP_ADDRESS = create(Set.class);
    //（预定义好的）表示http方法调用（上下文）参数key的AttributeKey对象
    public static final AttributeKey<ServiceInvoker> HTTP_INVOKER = create(ServiceInvoker.class);
    //静态代码块，用于将预定义好的AttributeKey对象及其名称放入namedMap集合中
    static{
        namedMap.put("IP_ADDRESS", IP_ADDRESS);
        namedMap.put("HTTP_INVOKER", HTTP_INVOKER);
    }
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.context
     * @Author: wyh
     * @Date: 2024-05-14 09:46
     * @Description: 静态内部类，用于实现AttributeKey抽象类。
     */
    public static class SimpleAttributeKey<T> extends AttributeKey<T>{
        //参数value的类型的class对象
        private Class<T> valueClass;
        /**
         * @date: 2024-05-14 9:48
         * @description: 有参构造器
         * @Param valueClass:
         * @return: null
         */
        SimpleAttributeKey(Class<T> valueClass) {
            this.valueClass = valueClass;
        }
        @Override
        public T cast(Object value) {
            return valueClass.cast(value);
        }
    }
    /**
     * @date: 2024-05-14 9:36
     * @description: 根据名称获取预定好的AttributeKey对象
     * @Param name:
     * @return: org.wyh.gateway.core.context.AttributeKey<?>
     */
    public static AttributeKey<?> valueOf(String name){
        return namedMap.get(name);
    }
    /**
     * @date: 2024-05-14 9:39
     * @description: 强制类型转换，用于将给定的对象转化成AttributeKey泛型指定的类型
     * @Param value:
     * @return: T
     */
    public abstract T cast(Object value);
    /**
     * @date: 2024-05-14 9:41
     * @description: 静态方法，用于创建表示参数key的AttributeKey对象。其中，泛型T指明了参数value的类型。
     * @Param valueClass: 参数value的类型的Class对象
     * @return: org.wyh.gateway.core.context.AttributeKey<T>
     */
    public static <T> AttributeKey<T> create(Class<? super T> valueClass){
        return new SimpleAttributeKey(valueClass);
    }
}
