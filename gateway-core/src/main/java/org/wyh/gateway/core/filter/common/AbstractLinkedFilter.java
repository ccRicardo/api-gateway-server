package org.wyh.gateway.core.filter.common;

import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.helper.ResponseHelper;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 16:25
 * @Description: 链式过滤器抽象类。链式过滤器能够组成一条过滤器链并且顺序执行。
                 （链式过滤器其实就是过滤器链上的节点）
                 注意，方法中的args参数实际上存放的是过滤器组件的配置类实例
 */
public abstract class AbstractLinkedFilter implements Filter {
    //指向下一个过滤器。默认为null。
    protected AbstractLinkedFilter next = null;
    /**
     * @date: 2024-05-14 16:44
     * @description: 设置该链式过滤器的下一个过滤器
     * @Param next: 
     * @return: void
     */
    public void setNext(AbstractLinkedFilter next){
        this.next = next;
    }
    /**
     * @date: 2024-05-14 16:45
     * @description: 获取该链式过滤器的下一个过滤器
     * @return: org.wyh.gateway.core.filter.common.AbstractLinkedFilter<T>
     */
    public AbstractLinkedFilter getNext(){
        return next;
    }

    @Override
    public void fireNext(GatewayContext ctx) throws Throwable{
        //根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
        if(ctx.isTerminated()){
            //（过滤器链中的某个组件执行异常）该过滤器链执行结束
            return;
        }
        if(ctx.isWritten()){
            //将响应结果写回客户端
            ResponseHelper.writeResponse(ctx);
        }
        //判断该过滤器是否是最后一个
        if(next != null){
            //检查是否要执行下一个过滤器
            if(next.check(ctx)){
                /*
                 * 执行下一个过滤器。
                 * 实际情况下，next指向的是一个过滤器实现类的实例，因此会尝试调用实现类实例的filter方法。
                 * 由于实现类中没有filter方法，所以会调用AbstractGatewayFilter的filter方法。
                 * 而AbstractGatewayFilter.filter在加载完配置参数后，又调用doFilter方法。
                 * 由于只有实现类中实现了doFilter方法，所以会去调用实现类的doFilter，真正完成过滤处理。
                 * 之后再调用该方法，激发下一个过滤器，并重复上述步骤，直至整个过滤器链执行完毕。
                 */
                next.filter(ctx);
            }else{
                //激发下下一个过滤器。此处是一个递归调用
                next.fireNext(ctx);
            }
        }else{
            //由于没有下一个过滤器了，所以将上下文状态设置为terminated
            ctx.setTerminated();
            return;
        }
    }
}
