package com.ih0rd.sandbox.users;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RandomUserResponse(
        @JsonProperty("results") List<Result> results
) {
    public record Result(
            @JsonProperty("gender") String gender,
            @JsonProperty("name") Name name,
            @JsonProperty("location") Location location,
            @JsonProperty("email") String email
    ) {
        public record Name(
                @JsonProperty("title") String title,
                @JsonProperty("first") String first,
                @JsonProperty("last") String last
        ) {}

        public record Location(
                @JsonProperty("street") Street street,
                @JsonProperty("city") String city,
                @JsonProperty("state") String state,
                @JsonProperty("country") String country,
                @JsonProperty("postcode") String postcode
        ) {
            public record Street(
                    @JsonProperty("number") int number,
                    @JsonProperty("name") String name
            ) {}
        }
    }
}
