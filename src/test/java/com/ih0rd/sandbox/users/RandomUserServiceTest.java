package com.ih0rd.sandbox.users;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class RandomUserServiceTest {

    @InjectMocks
    private RandomUserService randomUserService = Mockito.mock(RandomUserService.class);

    @Test
    public void testGetRandomUser() {
        var expectedResponse = TestUserGenerator.mockRandomUserResponse();

        when(randomUserService.getRandomUser())
                .thenReturn(expectedResponse);

        var randomUser = randomUserService.getRandomUser();

        Assertions.assertNotNull(randomUser);

        var results = randomUser.results();
        Assertions.assertEquals(1, results.size());

        var resultsFirst = results.getFirst();
        assertEquals("John", resultsFirst.name().first());
        assertEquals("Doe", resultsFirst.name().last());
    }
}
