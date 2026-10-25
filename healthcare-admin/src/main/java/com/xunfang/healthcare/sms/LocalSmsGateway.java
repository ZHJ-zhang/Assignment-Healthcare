package com.xunfang.healthcare.sms;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地模拟短信通道：只把短信内容写进日志，不真正下发到运营商。
 *
 * <p>用于开发调试与答辩演示，生产环境由第三方网关实现（阿里云 SMS / 腾讯云 SMS）替换。</p>
 */
@Slf4j
@Component
public class LocalSmsGateway implements SmsGateway {

    /**
     * 手机号格式校验，模拟通道同样按真实网关的入参要求校验。
     */
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    @Override
    public SmsResult send(String phone, String templateCode, String content) {
        if (StrUtil.isBlank(phone)) {
            return SmsResult.fail("手机号为空");
        }
        if (!phone.matches(PHONE_PATTERN)) {
            return SmsResult.fail("手机号格式不正确");
        }
        log.info("[模拟短信] 接收方={} 模板={} 内容={}", phone, templateCode, content);
        return SmsResult.ok();
    }
}
