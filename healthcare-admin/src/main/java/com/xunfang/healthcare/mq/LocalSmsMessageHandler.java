package com.xunfang.healthcare.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.entity.SysSmsRecord;
import com.xunfang.healthcare.mapper.SysSmsRecordMapper;
import com.xunfang.healthcare.sms.SmsGateway;
import com.xunfang.healthcare.sms.SmsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 短信主题处理器：调用短信通道下发，并把结果回写到发送记录。
 *
 * <p>发送状态 0 发送中、1 成功、2 失败，失败原因写入 error_msg。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalSmsMessageHandler implements MessageHandler {

    /**
     * 发送中。
     */
    public static final int STATUS_SENDING = 0;

    /**
     * 发送成功。
     */
    public static final int STATUS_SUCCESS = 1;

    /**
     * 发送失败。
     */
    public static final int STATUS_FAILED = 2;

    private final SmsGateway smsGateway;
    private final SysSmsRecordMapper smsRecordMapper;

    @Override
    public String topic() {
        return "sms";
    }

    @Override
    public void handle(Object payload) {
        Map<?, ?> data = MessagePayload.asMap(payload);
        String phone = MessagePayload.str(data, "phone");
        String templateCode = MessagePayload.str(data, "templateCode");
        String content = MessagePayload.str(data, "content");
        Long recordId = MessagePayload.longValue(data, "recordId");
        if (recordId == null) {
            log.warn("短信消息缺少发送记录 ID，无法回写状态");
            return;
        }

        SmsResult result;
        try {
            result = smsGateway.send(phone, templateCode, content);
        } catch (Exception e) {
            log.error("短信通道调用异常 phone={}", phone, e);
            result = SmsResult.fail("短信通道异常：" + e.getMessage());
        }
        writeBack(recordId, result);
    }

    /**
     * 回写发送状态与失败原因。
     */
    private void writeBack(Long recordId, SmsResult result) {
        int status = result.success() ? STATUS_SUCCESS : STATUS_FAILED;
        int rows = smsRecordMapper.update(null, Wrappers.<SysSmsRecord>lambdaUpdate()
                .eq(SysSmsRecord::getId, recordId)
                .set(SysSmsRecord::getSendStatus, status)
                .set(SysSmsRecord::getErrorMsg, result.success() ? null : result.errorMsg())
                .set(SysSmsRecord::getSendTime, LocalDateTime.now()));
        if (rows == 0) {
            log.warn("短信发送记录不存在或已回滚 recordId={}", recordId);
        } else {
            log.info("短信发送回写 recordId={} status={} errorMsg={}", recordId, status, result.errorMsg());
        }
    }
}
