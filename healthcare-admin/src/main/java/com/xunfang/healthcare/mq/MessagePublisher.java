package com.xunfang.healthcare.mq;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 消息发布抽象。当前由内置本地发布器实现（线程池异步投递）。
 */
public interface MessagePublisher {

    /**
     * 发布消息。
     *
     * @param topic   主题，例如 sms / notice / warning
     * @param payload 消息体
     */
    void publish(String topic, Object payload);

    /**
     * 发布消息；若当前处于事务中，则等待事务提交后再投递，
     * 避免业务回滚之后仍然把通知发出去。
     *
     * @param topic   主题
     * @param payload 消息体
     */
    default void publishAfterCommit(String topic, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(topic, payload);
                }
            });
            return;
        }
        publish(topic, payload);
    }
}
