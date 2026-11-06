package com.yunrui.edu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CoreConfiguration {
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
