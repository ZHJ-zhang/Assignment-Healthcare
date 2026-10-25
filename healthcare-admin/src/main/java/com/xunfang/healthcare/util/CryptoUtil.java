package com.xunfang.healthcare.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感字段加解密。
 *
 * <p>身份证号、手机号按需求文档 12.1「敏感字段加密存储」要求加密后落库，
 * 同时保留 SHA-256 摘要列用于精确检索（密文无法直接等值查询）。</p>
 */
@Slf4j
public final class CryptoUtil {

    /**
     * AES 密钥，生产环境通过环境变量 HEALTHCARE_AES_KEY 注入。
     */
    private static final String DEFAULT_KEY = "healthcare-aes-256-key-default";

    private CryptoUtil() {
    }

    /**
     * 派生固定长度的 AES-256 密钥。
     *
     * <p>AES 只接受 16 / 24 / 32 字节的密钥，而配置中的密钥长度不受约束，
     * 因此统一用 SHA-256 取前 32 字节做派生，保证加解密双方得到同一把密钥。</p>
     */
    private static String key() {
        String key = System.getenv("HEALTHCARE_AES_KEY");
        String secret = StrUtil.isBlank(key) ? DEFAULT_KEY : key;
        return DigestUtil.sha256Hex(secret).substring(0, 32);
    }

    /**
     * 加密敏感字段，空值原样返回。
     */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] keyBytes = key().getBytes(CharsetUtil.CHARSET_UTF_8);
            return SecureUtil.aes(keyBytes).encryptBase64(plain, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            log.error("敏感字段加密失败", e);
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    /**
     * 解密敏感字段；对非密文内容原样返回，便于历史数据兼容。
     */
    public static String decrypt(String cipher) {
        if (StrUtil.isBlank(cipher)) {
            return cipher;
        }
        try {
            byte[] keyBytes = key().getBytes(CharsetUtil.CHARSET_UTF_8);
            return SecureUtil.aes(keyBytes).decryptStr(cipher, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            return cipher;
        }
    }

    /**
     * 生成检索摘要，用于加密字段的精确匹配。
     */
    public static String digest(String plain) {
        if (StrUtil.isBlank(plain)) {
            return plain;
        }
        return DigestUtil.sha256Hex(plain.trim().toUpperCase());
    }

    /**
     * 手机号脱敏。
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证号脱敏。
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 2);
    }
}
