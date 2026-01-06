package com.yupi.springbootinit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class APPConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
