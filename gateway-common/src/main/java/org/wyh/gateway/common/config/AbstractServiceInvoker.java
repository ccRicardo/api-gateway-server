package org.wyh.gateway.common.config;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.config
 * @Author: wyh
 * @Date: 2024-01-17 14:24
 * @Description: 服务方法调用的抽象实现类（这里实际上并没有加abstract关键字）
                 方法调用指的是服务向外暴露/提供的可调用方法
 */
public class AbstractServiceInvoker implements ServiceInvoker{
    //方法调用全路径
    protected String invokerPath;
    //方法调用绑定的规则id
    protected String ruleId;
    //方法调用的超时时间
    protected int timeout = 5000;
    //方法的描述信息
    protected String desc;
    @Override
    public void setInvokerPath(String invokerPath) {
        this.invokerPath = invokerPath;
    }

    @Override
    public String getInvokerPath() {
        return this.invokerPath;
    }

    @Override
    public void setRuleId(String ruleId){
        this.ruleId = ruleId;
    }
    @Override
    public String getRuleId(){
        return this.ruleId;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public int getTimeout() {
        return this.timeout;
    }

    @Override
    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }


}
