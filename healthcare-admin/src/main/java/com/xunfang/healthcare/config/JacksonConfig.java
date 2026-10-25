package com.xunfang.healthcare.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置。
 *
 * <p>问题背景：实体主键使用 MyBatis-Plus {@code ASSIGN_ID}（雪花算法）生成，
 * 为 19 位 Long。JavaScript 的 Number 最大安全整数仅 2^53-1（约 16 位），
 * 前端 JSON.parse 时会丢失精度，导致把错误的主键回传给后端（例如把
 * 2114293114741018625 变成 2114293114741018600），从而出现"记录不存在"。</p>
 *
 * <p>解决：将所有<b>包装类型 Long</b> 序列化为字符串，保证前端拿到的是精确 id。
 * 注意仅注册 {@code Long.class}，不注册 {@code long.class}，因此分页的
 * {@code total/pageNum/pageSize} 等原始类型仍按数字输出，不受影响。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
