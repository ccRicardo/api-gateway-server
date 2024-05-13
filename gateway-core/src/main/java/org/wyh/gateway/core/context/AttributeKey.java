package org.wyh.gateway.core.context;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.context
 * @Author: wyh
 * @Date: 2024-05-13 16:59
 * @Description: 本类的作用就是表示/代表上下文参数的key。
                 Q: 为什么不直接使用String作为上下文参数（以下简称为参数）key的类型
                 A: 若直接使用String作为参数key的类型，则参数value的类型只能是Object
                    这会导致在设置和获取参数时不方便（设置时无法验证value的具体类型，获取时无法得知value的具体类型）
                    而单独定义一个类表示参数key，则可以通过使用泛型来指定/规定参数value的具体类型
 */
public class AttributeKey {

}
