package com.matrix.txproducer.config;

import com.matrix.txproducer.listener.OrderTransactionListener;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yihaosun
 * @date 2022/6/30 21:28
 */
@Component
public class TransactionProducer {
    private String producerGroup = "order_trans_group";

    // 事务消息
    private TransactionMQProducer producer;

    //用于执行本地事务和事务状态回查的监听器
    private final OrderTransactionListener orderTransactionListener;
    //执行任务的线程池
    ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(50));

    public TransactionProducer(OrderTransactionListener orderTransactionListener) {
        this.orderTransactionListener = orderTransactionListener;
    }

    @PostConstruct
    public void init(){
        producer = new TransactionMQProducer(producerGroup);
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.setSendMsgTimeout(Integer.MAX_VALUE);
        producer.setExecutorService(executor);
        // orderTransactionListener 第三步和第七步做的事
        producer.setTransactionListener(orderTransactionListener);
        this.start();
    }

    private void start(){
        try {
            this.producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    //事务消息发送
    public TransactionSendResult send(String data, String topic) throws MQClientException {
        Message message = new Message(topic,data.getBytes());
        return this.producer.sendMessageInTransaction(message, null);
    }
}
