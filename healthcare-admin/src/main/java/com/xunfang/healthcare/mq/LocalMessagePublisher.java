package com.xunfang.healthcare.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 内置消息发布器：线程池异步投递，消息处理失败只记录日志，不影响主流程。
 */
@Slf4j
@Component
public class LocalMessagePublisher implements MessagePublisher {

    private final ThreadPoolTaskExecutor executor;
    private final Map<String, MessageHandler> handlers;

    public LocalMessagePublisher(ThreadPoolTaskExecutor executor, List<MessageHandler> handlerList) {
        this.executor = executor;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(MessageHandler::topic, Function.identity(), (first, second) -> {
                    log.warn("消息主题重复注册：{}，保留先注册的处理器", first.topic());
                    return first;
                }));
    }

    @Override
    public void publish(String topic, Object payload) {
        MessageHandler handler = handlers.get(topic);
        if (handler == null) {
            log.warn("未注册的消息主题：{}", topic);
            return;
        }
        executor.execute(() -> {
            try {
                handler.handle(payload);
            } catch (Exception e) {
                log.error("消息处理失败 topic={}", topic, e);
            }
        });
    }
}
