package com.ih0rd.sandbox.users;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RandomService {

    public String randomUUIDString() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
