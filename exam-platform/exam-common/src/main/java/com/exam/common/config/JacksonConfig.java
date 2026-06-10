package com.exam.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Jackson 配置 — 解决 JavaScript 大数字精度丢失问题
 * 将 Long 类型的 ID 序列化为字符串，避免 JS 超过 Number.MAX_SAFE_INTEGER 精度丢失
 */
@Configuration
public class JacksonConfig {

    /** 需要序列化为字符串的字段名 */
    private static final List<String> ID_FIELD_NAMES = Arrays.asList("id", "projectId", "creatorId", "userId", "paperId", "toolId");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 将 Long 类型序列化为字符串
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
