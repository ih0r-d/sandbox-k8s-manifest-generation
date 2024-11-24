package com.ih0rd.sandbox.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RandomUserService {

    private final RestClient restClient;

    public RandomUserService(@Value("${random-users.uri}") String uri) {
        this.restClient =  RestClient.builder()
                .baseUrl(uri)
                .build();
    }

    public RandomUserResponse getRandomUser() {
        return restClient.get()
                .uri("/api/")
                .retrieve()
                .body(RandomUserResponse.class);
    }
}
