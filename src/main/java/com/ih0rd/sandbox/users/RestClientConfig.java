package com.ih0rd.sandbox.users;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.HttpExchange;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient randomUserClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://randomuser.me")
                .build();
    }
}
