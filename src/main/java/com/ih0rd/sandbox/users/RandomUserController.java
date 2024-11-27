package com.ih0rd.sandbox.users;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
public class RandomUserController {

    private final RandomUserService randomUserService;
    private final RandomService randomService;

    public RandomUserController(RandomUserService randomUserService, RandomService randomService) {
        this.randomUserService = randomUserService;
        this.randomService = randomService;
    }

    @GetMapping("/random-user")
    public RandomUserResponse getRandomUser() {
        return randomUserService.getRandomUser();
    }

    @GetMapping("/secure")
    public String secureRandomText() {
        return "[HTTPS] " + randomService.randomUUIDString();
    }



}