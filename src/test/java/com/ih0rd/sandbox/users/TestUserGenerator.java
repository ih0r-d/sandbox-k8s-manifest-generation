package com.ih0rd.sandbox.users;

import java.util.List;

public class TestUserGenerator {

    public static RandomUserResponse mockRandomUserResponse() {
        return new RandomUserResponse(
                List.of(new RandomUserResponse.Result(
                        "male",
                        new RandomUserResponse.Result.Name("Mr", "John", "Doe"),
                        new RandomUserResponse.Result.Location(
                                new RandomUserResponse.Result.Location.Street(123, "Main Street"),
                                "Springfield",
                                "Illinois",
                                "USA",
                                "62701"
                        ),
                        "johndoe@example.com"
                ))
        );
    }
}
