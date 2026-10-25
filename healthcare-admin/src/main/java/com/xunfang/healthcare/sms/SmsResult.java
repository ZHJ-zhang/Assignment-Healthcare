package com.xunfang.healthcare.sms;

/**
 * 短信发送结果。
 *
 * @param success  是否发送成功
 * @param errorMsg 失败原因，发送成功时为 null
 */
public record SmsResult(boolean success, String errorMsg) {

    /**
     * 发送成功。
     */
    public static SmsResult ok() {
        return new SmsResult(true, null);
    }

    /**
     * 发送失败。
     */
    public static SmsResult fail(String errorMsg) {
        return new SmsResult(false, errorMsg);
    }
}
