package com.xunfang.healthcare.mq;

import com.xunfang.healthcare.entity.SysMessage;
import com.xunfang.healthcare.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 站内消息主题处理器：把消息写入 sys_message。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalInboxMessageHandler implements MessageHandler {

    private final SysMessageMapper messageMapper;

    @Override
    public String topic() {
        return "message";
    }

    @Override
    public void handle(Object payload) {
        Map<?, ?> data = MessagePayload.asMap(payload);
        SysMessage message = new SysMessage();
        message.setReceiverId(MessagePayload.longValue(data, "receiverId"));
        message.setTitle(MessagePayload.str(data, "title"));
        message.setContent(MessagePayload.str(data, "content"));
        message.setMsgType(MessagePayload.intValue(data, "msgType", 1));
        message.setIsRead(0);
        message.setBizId(MessagePayload.str(data, "bizId"));
        messageMapper.insert(message);
        log.info("站内消息已投递 receiverId={} title={}", message.getReceiverId(), message.getTitle());
    }
}
