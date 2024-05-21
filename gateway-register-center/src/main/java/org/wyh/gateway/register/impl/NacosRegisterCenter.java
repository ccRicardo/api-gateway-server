package org.wyh.gateway.register.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.constant.GatewayConst;
import org.wyh.gateway.register.api.RegisterCenter;
import org.wyh.gateway.register.api.RegisterCenterListener;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.register.center.nacos
 * @Author: wyh
 * @Date: 2024-01-24 10:51
 * @Description: 注册中心的nacos实现
 */
@Slf4j
public class NacosRegisterCenter implements RegisterCenter {
    //注册中心的地址（具体指Nacos服务器的地址）
    private String registerAddress;
    //环境（例如开发/测试环境）
    private String env;
    //用于维护和管理服务定义信息
    private NamingMaintainService namingMaintainService;
    //用于维护和管理服务实例信息
    private NamingService namingService;
    //（自定义）监听器列表（这些监听器会在nacos事件监听器中的onEvent方法中被调用）
    private List<RegisterCenterListener> listenerList = new CopyOnWriteArrayList<>();

    @Override
    public void init(String registerAddress, String env) {
        this.registerAddress = registerAddress;
        this.env = env;
        try{
            //通过相应工厂类创建NamingMaintainService和NamingService实例
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
            this.namingService = NamingFactory.createNamingService(registerAddress);
        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try{
            //构造nacos服务实例。Instance代表nacos中的一个服务实例
            Instance nacosInstance = new Instance();
            //设置nacos服务实例的相关信息
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setPort(serviceInstance.getPort());
            nacosInstance.setIp(serviceInstance.getIp());
            //设置元数据，通常是添加一些自定义的属性。（这里直接把自定义的ServiceInstance对象当成了元数据）
            nacosInstance.setMetadata(Map.of(GatewayConst.META_DATA_KEY,
                    JSON.toJSONString(serviceInstance)));
            //注册nacos服务实例。三个参数分别是：服务的名称，服务所属的分组名称和服务实例
            namingService.registerInstance(serviceDefinition.getServiceId(), env, nacosInstance);
            //更新服务定义信息。四个参数分别是：服务名称，服务所属分组名称，
            //服务的保护阈值（若服务的健康实例数低于该值，Nacos会拒绝所有实例的注销请求），元数据
            namingMaintainService.updateService(serviceDefinition.getServiceId(), env, 0,
                    Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition)));
            log.info("【注册中心】注册服务 {} {}", serviceDefinition, serviceInstance);
            //System.out.println("********************************");
        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try{
            namingService.deregisterInstance(serviceDefinition.getServiceId(),
                    env, serviceInstance.getIp(), serviceInstance.getPort());
        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeServicesChange(RegisterCenterListener listener) {
        //添加监听器
        listenerList.add(listener);
        //将服务订阅的实现委托给doSubscribeAllServices方法
        doSubscribeAllServices();
        /*
         * 由于可能会有新的服务加入，所以需要设置定时任务来定期执行doSubscribeAllServices方法，
         * 以免遗漏了后续新增服务的订阅
         * 下面的代码先是创建了一个包含一个线程的定时任务线程池。
         * 然后将定时任务设为每隔10s执行一次doSubscribeAllServices方法
         */
        ScheduledExecutorService scheduledThreadPool = Executors
                .newScheduledThreadPool(1, new NameThreadFactory("doSubscribeAllServices"));
        scheduledThreadPool.scheduleWithFixedDelay(() -> doSubscribeAllServices(),
                10, 10, TimeUnit.SECONDS);

    }
    /**
     * @date: 2024-01-24 15:20
     * @description: 实现服务订阅的业务逻辑。服务订阅就是监听服务实例的变动/更新，然后进行相应的处理。
     * @return: void
     */
    private void doSubscribeAllServices(){
        try{
            /*
             * namingService.getSubscribeServices方法能够获取所有已订阅服务的服务信息
             * stream将上述方法返回的ServiceInfo列表转换为流（stream）
             * map将ServiceInfo::getName方法应用到流中的每个元素上，然后返回一个对应结果组成的新流。
             * collect(Collectors.toSet())将流中的元素收集到一个Set集合中
             * 因此，这行代码的作用就是得到已订阅服务的名称的Set集合
             */
            Set<String> subscribeService = namingService.getSubscribeServices().stream()
                    .map(ServiceInfo::getName).collect(Collectors.toSet());
            //设置分页的当前页码
            int pageNo = 1;
            //设置分页的页大小
            int pageSize = 100;
            //从nacos服务器中分页获取（已注册）服务列表
            List<String> serviceList = namingService
                    .getServicesOfServer(pageNo, pageSize, env).getData();
            while (CollectionUtils.isNotEmpty(serviceList)) {
                log.info("【注册中心】服务列表大小: {}", serviceList.size());
                for (String service : serviceList) {
                    //如果该服务已经订阅，则跳过
                    if (subscribeService.contains(service)) {
                        continue;
                    }
                    //设置nacos事件监听器。
                    EventListener eventListener = new NacosRegisterListener();
                    //由于服务状态即将发生变化（订阅新服务），所以要调用nacos事件监听器的onEvent方法。
                    eventListener.onEvent(new NamingEvent(service, null));
                    //订阅该服务。
                    namingService.subscribe(service, env, eventListener);
                    log.info("【注册中心】订阅服务 {} {}", service, env);
                }
                //分页获取剩下的服务列表
                serviceList = namingService
                        .getServicesOfServer(++pageNo, pageSize, env).getData();
            }
        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.gateway.register.center.nacos
     * @Author: wyh
     * @Date: 2024-01-24 15:43
     * @Description: nacos事件监听器实现类（内部类）
                     当已订阅服务的状态发生变化时，Nacos会触发一个事件，然后调用该监听器中的onEvent方法。
     */
    public class NacosRegisterListener implements EventListener{

        @Override
        public void onEvent(Event event) {
            //判断服务的状态是否发生变化。（当服务状态发生变化时，nacos会触发一个NamingEvent事件）
            if(event instanceof NamingEvent){
                NamingEvent namingEvent = (NamingEvent) event;
                //获取namingEvent中的“服务名称”属性。该名称由环境名与服务名拼接而成，形如dev@@http-service
                String serviceName = namingEvent.getServiceName();
                try{
                    //获取服务定义信息
                    Service service = namingMaintainService.queryService(serviceName, env);
                    //nacos Service中的元数据是序列化后的ServiceDefinition对象。因此，这里要反序列化。
                    ServiceDefinition serviceDefinition = JSON.parseObject(service.getMetadata()
                            .get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);
                    //通过服务名称获取服务实例信息。注意不要使用serviceName！
                    List<Instance> allInstances = namingService.getAllInstances(service.getName(), env);
                    Set<ServiceInstance> instanceSet = new HashSet<>();
                    for (Instance instance : allInstances) {
                        //nacos Instance中的元数据是序列化后的ServiceInstance对象
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata()
                                .get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        instanceSet.add(serviceInstance);
                    }
                    //调用（自定义）监听器列表中各监听器的onChange方法
                    //forEach方法能够对流中的每个元素执行lambda表达式（以元素本身作为入参）
                    listenerList.stream()
                            .forEach(listener -> listener.onChange(serviceDefinition, instanceSet));
                }catch (NacosException e){
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
