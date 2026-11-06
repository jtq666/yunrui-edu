package com.yunrui.edu.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 浏览器只能安全表示到 2^53-1 的整数。订单号等长整型统一按字符串输出，
 * 防止雪花编号在前端被四舍五入后无法查询原订单。
 */
@Configuration
public class JsonConfiguration {
    @Bean
    Module longIdAsStringModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return module;
    }
}
