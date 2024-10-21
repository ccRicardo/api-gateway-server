package org.wyh.gateway.core.bufferqueue.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.bufferqueue.api.BufferQueue;
import org.wyh.gateway.core.bufferqueue.api.EventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
 * @Author: wyh
 * @Date: 2024-03-20 15:36
 * @Description: 基于Disruptor框架实现的高性能缓冲队列。其中泛型E指的是事件中数据的类型。
                 Disruptor是一个高性能的内存队列。其高性能主要得益于两点：
                 1、使用了CAS原语来替代（悲观）锁，来保证多线程安全
                 2、通过填充缓存行，使每个变量都处于不同的缓存行中，这样就能保证每个变量都不会与其他任何变量产生意外冲突，避免了伪共享问题
                 总而言之，Disruptor是一个基于数组实现，并使用CAS操作保证多线程安全的环形内存队列。
                 本系统中：
                 1、生产者是DisruptorNettyCoreProcessor中的process方法
                 2、消费者是本类中的DisruptorWorkHandler.onEvent方法。
                 该方法会调用DisruptorNettyCoreProcessor.onEvent方法，来完成请求事件的处理。
 */
@Slf4j
public class DisruptorBufferQueue<E> implements BufferQueue<E> {
    /*
     * Disruptor中的一些关键概念：
     * 1、事件：包含了生产者要传递给消费者的数据/值。在本系统中，事件的值具体是指HttpRequestWrapper对象。
     * 注意：本类在编写的时候并未严格区分事件和事件的数据/值，因此遇到歧义时需要仔细辨别。
     * 实际上，Event类型的参数是事件，而E类型的参数是事件的数据/值。
     * 2、RingBuffer：Disruptor中用于存储事件的数据结构，其本质是一个基于数组实现的环形缓冲队列。
     * 生产者将事件放入缓冲队列，随后，消费者从缓冲队列中取出该事件进行处理。
     * 3、Sequence（序列号）：本质上其实是对RingBuffer中位置下标的抽象/封装，在发布和消费事件时用于指明事件对象的位置
     * 4、Sequencer（序列器）：Disruptor的控制核心。
     * 用于控制RingBuffer中事件的读写等行为，协调生产者与消费者之间的事件传递。
     * Sequencer中维护了两个重要的Sequence：
     * 用于记录生产者生产进度位置的cursor和用于记录消费者消费进度/位置的gatingSequence。
     * 生产方面，Sequencer需要保证不会覆盖掉未被消费的事件，即cursor+1不能超过gatingSequence。
     * 消费方面，Sequencer需要判断是否有可消费的事件，即检查gatingSequence和cursor的大小关系。
     * 5、SequenceBarrier（序列屏障）：用于维护消费者与生产者，或者与其他消费者之间的依赖关系。
     * （依赖关系有两种：消费者依赖于生产者，或者消费者依赖于其他的消费者）
     * 具体来说，当消费者请求消费指定Sequence位置的事件时，SequenceBarrier会将其与dependentSequence比较。
     * （dependentSequence默认为Sequencer中的生产者cursor，也可以设置为被依赖消费者的Sequence。）
     * 如果dependentSequence大于指定的Sequence，则返回不大于dependentSequence的最大可消费的位置。
     * 否则，根据指定的等待策略，让对应消费者进行等待。
     * 6、生产者：用于初始化并发布事件的对象。
     * 通常通过事件转换器EventTranslator来初始化事件，再通过RingBuffer.publishEvent发布该事件。
     * 事件初始化分为两步：先从Sequencer获取Sequence，然后给RingBuffer相应位置的事件对象赋值/填充数据。
     * 以上两步都被EventTranslator封装好了，因此只需要重写相应方法，实现具体的赋值操作即可。
     * 发布事件本质上就是通知RingBuffer该事件已经写入完毕。
     * 7、消费者：用于处理缓冲队列中事件的对象。
     * Disruptor会通过EventProcessor不断地从RingBuffer中获取事件，并传递给EventHandler的onEvent回调方法进行处理。
     * EventHandler.onEvent回调方法的业务逻辑由用户来具体实现。
     * EventProcessor和EventHandler与WorkProcessor和WorkHandler的区别在于前者不需要池化，后者需要池化。
     */
    //Disruptor中存储事件的环形缓冲队列
    private RingBuffer<Event> ringBuffer;
    //缓冲队列的事件监听器
    private EventListener<E> eventListener;
    //线程池，供下面的消费者池使用。当消费者池启动时，会传入该线程池，并给每个WorkPrcessor（消费者）分配一个线程。
    private ExecutorService executorService;
    //消费者池，用于管理一组WorkProcessor对象（也就是消费者）。其中，每个WorkPrcessor都需要一个单独的执行线程。
    private WorkerPool<Event> workerPool;
    //事件转换器，用于初始化事件
    private EventTranslatorOneArg<Event, E> eventTranslator;
    /**
     * @date: 2024-03-22 15:20
     * @description: private修饰的构造器。使用建造者模式，通过Builder.build方法来调用。
     * @Param builder:
     * @return: null
     */
    private DisruptorBufferQueue(Builder<E> builder){
        /*
         * 注意：这里的ringBuffer不是属性，只是一个局部变量。ringBuffer属性的赋值在后面的start方法中。
         * 然而实际上，属性和此处的局部变量引用的却是同一个RingBuffer对象。
         * 因为在创建WorkerPool对象时，局部变量引用的RingBuffer对象也同时被WorkerPool对象中的ringBuffer属性引用。
         * 而WorkerPool.start方法返回的正是其ringBuffer属性引用的RingBuffer对象。
         */
        RingBuffer<Event> ringBuffer = RingBuffer.create(builder.producerType, new DisruptorEventFactory(),
                builder.bufferSize, builder.waitStrategy);
        this.eventListener = builder.listener;
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                //设置每个线程的名称，其中%d会被自动替换为一个递增的整数
                new ThreadFactoryBuilder().setNameFormat(builder.namePrefix+"-%d").build());
        //创建SequenceBarrier，用于后续消费者池WorkerPool的构建
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        //创建WorkHandler事件处理器组，用于后续消费者池WorkerPool的构建
        WorkHandler<Event>[] workHandlers = new WorkHandler[builder.threads];
        for (int i = 0; i < workHandlers.length; i++) {
            workHandlers[i] = new DisruptorWorkHandler();
        }
        this.workerPool = new WorkerPool(ringBuffer, sequenceBarrier,
                new DisruptorExceptionHandler(), workHandlers);
        /*
         * 在RingBuffer对应的Sequencer中设置gatingSequences属性（其作用在上述关键概念中有提到）
         * 使其能够追踪到WorkerPool中每个WorkProcessor消费者的事件处理/消费进度，
         * （WorkerPool.getWorkerSequences会返回一个Sequence数组，该数组记录了每一个WorkProcessor的消费进度/位置）
         * 从而确保生产者在发布事件时不会覆盖还未被消费的事件。
         */
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        this.eventTranslator = new DisruptorEventTranslator();
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
     * @Author: wyh
     * @Date: 2024-03-22 14:30
     * @Description: 静态内部类。建造者类，用于实现建造者模式，以简化本类对象的创建。
     */
    public static class Builder<E>{
        /*
         * 生产者类型。本系统默认为多生产者，因为netty server中的worker eventLoopGroup是多线程的。
         * 在多生产者环境下，每个生产者需要先通过CAS操作竞争获取到可写空间，然后再往里放事件。
         * 故合理推测：netty server中的线程数越多，生产者写入事件时发生冲突的可能性就越大。
         * 因此，系统的整体吞吐量不一定会增大。
         */
        // TODO: 2024-10-21 根据Single Writer Principle策略，好像单生产者的并发想能更好 
        private ProducerType producerType = ProducerType.MULTI;
        //RingBuffer的大小。注意必须为2的n次方。
        private int bufferSize = 16 * 1024;
        //消费者线程的数量
        private int threads = 1;
        //消费者线程的名称前缀
        private String namePrefix = "缓冲队列消费者线程";
        //等待策略。默认为阻塞等待。（目前Disruptor只针对消费者等待生产者的情况应用了等待策略）
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();
        //缓冲队列的事件监听器
        private EventListener<E> listener;
        /*
         * 以下是一系列的set方法，用于设置上述属性。由于重复性过高，就不再分别注释了。
         * 注意，这些set方法不能使用@Setter生成，因为要保证set方法返回的是Builder对象自身。
         * 这样，才能使用链式方式设置Builder对象的属性。
         */
        public Builder<E> setProducerType(ProducerType producerType) {
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setListener(EventListener<E> listener) {
            this.listener = listener;
            return this;
        }
        /**
         * @date: 2024-03-22 15:09
         * @description: 建造方法，根据当前的Builder对象构建复杂的DisruptorBufferQueue对象。
         * @return: org.wyh.core.bufferqueue.disruptor.DisruptorBufferQueue<E>
         */
        public DisruptorBufferQueue<E> build(){
            return new DisruptorBufferQueue(this);
        }

    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
     * @Author: wyh
     * @Date: 2024-03-21 10:39
     * @Description: 内部类，用于定义Disruptor中的事件，本质上就是生产者通过Disruptor提供给消费者的数据。
     */
    @Data
    private class Event{
        private E value;
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
     * @Author: wyh
     * @Date: 2024-03-21 15:37
     * @Description: 内部类，用于定义事件工厂，来创建事件对象。
     */
    private class DisruptorEventFactory implements EventFactory<Event>{
        @Override
        public Event newInstance() {
            return new Event();
        }
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
     * @Author: wyh
     * @Date: 2024-03-21 15:57
     * @Description: 内部类，用于定义事件转换器，来初始化事件。
                     其中，translateTo回调方法会在发布事件时被调用，用于给相应的事件对象赋值/填充数据
     */
    private class DisruptorEventTranslator implements EventTranslatorOneArg<Event, E> {
        @Override
        public void translateTo(Event event, long sequence, E value) {
            //给事件赋值/填充数据（其实就是调用set方法给value属性赋值）
            event.setValue(value);
        }
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
     * @Author: wyh
     * @Date: 2024-03-21 14:04
     * @Description: 内部类，用于定义异常处理器，对消费者处理事件过程中出现的异常进行处理
                     其中的3个回调方法会在Disruptor中出现相应的异常时被调用
                     ExceptionHandler方法中参数的意义如下：
                     throwable：异常对象 sequence：事件的序列号 event：事件对象
     */
    private class DisruptorExceptionHandler implements ExceptionHandler<Event>{

        @Override
        public void handleEventException(Throwable ex, long sequence, Event event) {
            //该方法用于对事件处理/消费过程中产生的异常进行处理
            try{
                //调用事件监听器中的onException方法处理异常
                eventListener.onException(ex, sequence, event.value);
            }catch (Exception e){
                //在事件监听器中已经进行了完善的异常处理，这里不再需要处理，只是用作占位
            }finally {
                //将事件的value属性（即数据）设置为null，表示该事件已经被处理完毕
                event.setValue(null);
            }

        }

        @Override
        public void handleOnStartException(Throwable ex) {
            //该方法用于对Disruptor启动过程中产生的异常进行处理
            log.error("Disruptor启动异常");
            throw new RuntimeException(ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            //该方法用于对Disruptor关闭过程中产生的异常进行处理
            log.error("Disruptor关闭异常");
            throw new RuntimeException(ex);
        }
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.bufferqueue.disruptor
     * @Author: wyh
     * @Date: 2024-03-21 14:50
     * @Description: 内部类，用于定义事件处理器（也就是消费者），来消费/处理RingBuffer中的事件。
                     其中的onEvent回调方法会在消费者线程从RingBuffer中获取到事件时被调用。
                     当onEvent方法执行过程中出现异常时，会调用上述异常处理器中的handleEventException方法。
     */
    private class DisruptorWorkHandler implements WorkHandler<Event>{

        @Override
        public void onEvent(Event event) throws Exception {

            //调用事件监听器中的onEvent方法处理事件
            eventListener.onEvent(event.value);
            //将事件的value属性（即数据）设置为null，表示该事件已经被处理完毕
            event.setValue(null);
        }
    }
    /**
     * @date: 2024-03-25 9:40
     * @description: 调用事件监听器中的onException方法，处理缓冲队列中单个事件引发的异常
     * @Param listener:
     * @Param ex:
     * @Param eventData:
     * @return: void
     */
    private static <E> void process(EventListener<E> listener, Throwable ex, E eventValue){
        //该方法中没有传入Sequence参数，而且监听器的onException方法也不需要，因此这里设置为-1，表示该参数无效/不存在
        listener.onException(ex, -1, eventValue);
    }
    /**
     * @date: 2024-03-25 9:41
     * @description: 调用事件监听器中的onException方法，处理缓冲队列中多个事件引发的异常
     * @Param listener: 
     * @Param ex: 
     * @Param eventDatas: 
     * @return: void
     */
    private static <E> void process(EventListener<E> listener, Throwable ex, E... eventValues){
        for (E eventValue : eventValues) {
            process(listener, ex, eventValue);
        }
    }
    @Override
    public void add(E eventValue) {
        if(ringBuffer == null){
            process(eventListener, new IllegalStateException("缓冲队列未开启"), eventValue);
        }
        try{
            //初始化并发布事件
            ringBuffer.publishEvent(eventTranslator, eventValue);
        }catch (NullPointerException e){
            process(eventListener, new IllegalStateException("缓冲队列未开启"), eventValue);
        }
    }

    @Override
    public void add(E... eventValues) {
        if(ringBuffer == null){
            process(eventListener, new IllegalStateException("缓冲队列未开启"), eventValues);
        }
        try{
            //初始化并发布多个事件
            ringBuffer.publishEvents(eventTranslator, eventValues);
        }catch(NullPointerException e){
            process(eventListener, new IllegalStateException("缓冲队列未开启"), eventValues);
        }
    }

    @Override
    public boolean tryAdd(E eventValue) {
        //若事件添加成功则返回true，否则一律返回false
        if(ringBuffer == null){
            return false;
        }
        try{
            return ringBuffer.tryPublishEvent(eventTranslator, eventValue);
        }catch(NullPointerException e){
            return false;
        }
    }

    @Override
    public boolean tryAdd(E... eventValues) {
        //若多个事件添加成功则返回true，否则一律返回false
        if(ringBuffer == null){
            return false;
        }
        try{
            return ringBuffer.tryPublishEvents(eventTranslator, eventValues);
        }catch(NullPointerException e){
            return false;
        }
    }

    @Override
    public void start() {
        //启动消费者池，并返回RingBuffer对象，供ringBuffer属性引用。
        this.ringBuffer = workerPool.start(executorService);
        log.info("Disruptor缓冲队列启动成功");
    }

    @Override
    public void shutDown() {
        //若ringBuffer属性为null，则表示缓冲队列已经关闭（或者说未开启）
        if(ringBuffer == null){
            return;
        }
        //关闭消费者池
        if(workerPool != null){
            workerPool.drainAndHalt();
        }
        //关闭线程池
        if(executorService != null){
            executorService.shutdown();
        }
        //将ringBuffer属性设置为null，表示缓冲队列已经关闭
        ringBuffer = null;
        log.info("Disruptor缓冲队列关闭成功");
    }

    @Override
    public boolean isShutDown() {
        //若ringBuffer属性为null，则表示缓冲队列已经关闭（或者说未开启）
        return ringBuffer == null;
    }


}
