package com.xunfang.healthcare.service;

import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知编排：把业务事件翻译成短信与站内消息。
 *
 * <p>通知属于旁路能力，发送失败只记录日志，不影响主业务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    /**
     * 短信落款。
     */
    private static final String SMS_SIGNATURE = "【医疗预约系统】";

    private final BizPatientMapper bizPatientMapper;
    private final MessageService messageService;

    /**
     * 给患者发短信，患者不存在时跳过。
     */
    public void smsToPatient(Long patientId, String templateCode, String content) {
        BizPatient patient = patientId == null ? null : bizPatientMapper.selectById(patientId);
        smsToPatient(patient, templateCode, content);
    }

    /**
     * 给患者发短信；患者未登记手机号时跳过。
     */
    public void smsToPatient(BizPatient patient, String templateCode, String content) {
        if (patient == null) {
            log.warn("患者不存在，跳过短信发送");
            return;
        }
        String phone = CryptoUtil.decrypt(patient.getPhone());
        if (StrUtil.isBlank(phone)) {
            log.warn("患者 {} 未登记手机号，跳过短信发送", patient.getPatientNo());
            return;
        }
        try {
            messageService.sendSms(phone, templateCode, SMS_SIGNATURE + content);
        } catch (Exception e) {
            log.error("短信发送失败 patientId={}", patient.getId(), e);
        }
    }

    /**
     * 给指定用户发站内消息，缺少接收人时跳过。
     */
    public void inboxToUser(Long receiverId, String title, String content, Integer msgType, String bizId) {
        if (receiverId == null) {
            log.warn("站内消息缺少接收人，已跳过 title={}", title);
            return;
        }
        try {
            messageService.sendMessage(receiverId, title, content, msgType, bizId);
        } catch (Exception e) {
            log.error("站内消息发送失败 receiverId={} title={}", receiverId, title, e);
        }
    }
}
