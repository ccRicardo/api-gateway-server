package org.wyh.gateway.core.filter.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.filter.AbstractLinkedFilter;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-15 13:43
 * @Description: 网关的过滤器链定义类。
                 Q: 为什么网关过滤器链中的节点类型是AbstractLinkedFilter而不是AbstractGatewayFilter
                 A: 因为后者的抽象层次太低了，需要实例化表示过滤器配置类的泛型参数。
                 而过滤器链中不同过滤器的配置实现类肯定不同，所以会导致相关接口的设计更复杂（需要多提供一个参数）
 */
@Slf4j
public class GatewayFilterChain {
    //过滤器链id
    @Getter
    private String filterChainId;
    //头指针，指向第一个过滤器。由于本类中使用了虚拟头节点，所以该指针始终指向虚拟头节点。
    private AbstractLinkedFilter<GatewayContext> first;
    //尾指针，指向最后一个过滤器
    private AbstractLinkedFilter<GatewayContext> end;
    /**
     * @date: 2024-05-15 14:09
     * @description: 有参构造器。完成属性初始化工作。
     * @Param filterChainId:
     * @return: null
     */
    public GatewayFilterChain(String filterChainId){
        this.filterChainId = filterChainId;
        //创建虚拟头节点，简化链表相关操作
        this.first = new AbstractLinkedFilter<GatewayContext>() {
            @Override
            public boolean check(GatewayContext gatewayContext) {
                //该方法永远不会执行
                return false;
            }
            @Override
            public void doFilter(GatewayContext gatewayContext, Object... args) {
                //该方法永远不会执行
            }
        };
        //初始状态下，尾指针指向虚拟头节点
        this.end = this.first;
    }
    /**
     * @date: 2024-05-15 14:35
     * @description: 在过滤器链的头部之前添加一个过滤器（添加到虚拟头节点之后）
     * @Param filter:
     * @return: void
     */
    public void addFirst(AbstractLinkedFilter<GatewayContext> filter){
        filter.setNext(first.getNext());
        first.setNext(filter);
        //如果链表之前为空，则更新尾指针
        if(first == end){
            end = filter;
        }
    }
    /**
     * @date: 2024-05-15 14:35
     * @description: 在过滤器的尾部之后添加一个过滤器
     * @Param filter:
     * @return: void
     */
    public void addLast(AbstractLinkedFilter<GatewayContext> filter){
        end.setNext(filter);
        end = filter;
    }
    /**
     * @date: 2024-05-15 14:38
     * @description: 启动过滤器链，即激发第一个过滤器实例。
                     由于过滤器实例的doFilter方法会调用fireNext方法，所以只要激发第一个过滤器实例，就能自动地顺序执行整个过滤器链
     * @Param ctx: 
     * @Param args: 
     * @return: void
     */
    public void start(GatewayContext ctx){
        //激发第一个过滤器实例（不激发虚拟头节点）
        first.fireNext(ctx);
    }



}
