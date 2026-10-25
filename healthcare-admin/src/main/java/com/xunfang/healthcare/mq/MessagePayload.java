package com.xunfang.healthcare.mq;

import java.util.Map;

/**
 * 消息负载工具：发布方统一用 Map 传递消息体，这里做类型安全的读取。
 */
public final class MessagePayload {

    private MessagePayload() {
    }

    /**
     * 把消息体转成 Map，非 Map 返回空 Map。
     */
    public static Map<?, ?> asMap(Object payload) {
        return payload instanceof Map<?, ?> data ? data : Map.of();
    }

    /**
     * 读取字符串。
     */
    public static String str(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取 Long。
     */
    public static Long longValue(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    /**
     * 读取 Integer，取不到时返回默认值。
     */
    public static Integer intValue(Map<?, ?> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }
}
