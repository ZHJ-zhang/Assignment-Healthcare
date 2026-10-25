package com.xunfang.healthcare.sms;

/**
 * 短信通道抽象。
 *
 * <p>当前由 {@link LocalSmsGateway}（本地模拟发送）实现，不依赖任何第三方凭据。
 * 生产环境接入阿里云 / 腾讯云时，只需新增一个 {@code SmsGateway} 实现并替换 Bean，
 * 业务代码与消息处理器都不需要改动。</p>
 */
public interface SmsGateway {

    /**
     * 发送短信。
     *
     * @param phone        接收方手机号
     * @param templateCode 模板编码
     * @param content      短信内容
     * @return 发送结果，失败时携带失败原因
     */
    SmsResult send(String phone, String templateCode, String content);
}
