package org.wyh.gateway.core.filter.common;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 16:25
 * @Description: 链式过滤器抽象类。链式过滤器能够组成一条过滤器链并且顺序执行。
                 （链式过滤器其实就是过滤器链上的节点）
                 其中，泛型T指的是过滤器处理的内容的类型
 */
public abstract class AbstractLinkedFilter<T> implements Filter<T> {
    //指向下一个过滤器。默认为null。
    protected AbstractLinkedFilter<T> next = null;
    /**
     * @date: 2024-05-14 16:44
     * @description: 设置该链式过滤器的下一个过滤器
     * @Param next: 
     * @return: void
     */
    public void setNext(AbstractLinkedFilter<T> next){
        this.next = next;
    }
    /**
     * @date: 2024-05-14 16:45
     * @description: 获取该链式过滤器的下一个过滤器
     * @return: org.wyh.gateway.core.filter.common.AbstractLinkedFilter<T>
     */
    public AbstractLinkedFilter<T> getNext(){
        return next;
    }

    @Override
    public void filter(T t, Object... args) {
        //该方法是一个接口方法，因此，此处实际上调用的是具体实现类（即具体过滤器）的filter方法。
        doFilter(t, args);
    }

    @Override
    public void fireNext(T t, Object... args) {
        //判断该过滤器是否是最后一个
        if(next != null){
            //检查是否要执行下一个过滤器
            if(next.check(t)){
                //执行下一个过滤器
                next.filter(t, args);
            }else{
                //激发下下一个过滤器
                next.fireNext(t, args);
            }
        }else{
            //由于没有下一个过滤器了，所以直接返回
            return;
        }
    }
}
