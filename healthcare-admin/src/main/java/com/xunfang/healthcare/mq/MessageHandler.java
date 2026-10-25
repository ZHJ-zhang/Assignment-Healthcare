package com.xunfang.healthcare.mq;

/**
 * 消息处理器，topic 与 handler 一一对应。
 */
public interface MessageHandler {

    /**
     * 本处理器订阅的消息主题。
     */
    String topic();

    /**
     * 处理消息。
     */
    void handle(Object payload);
}
