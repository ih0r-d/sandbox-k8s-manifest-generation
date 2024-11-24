package com.ih0rd.sandbox.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RandomUserController.class)
public class RandomUserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RandomUserService randomUserService;

    @Test
    public void testGetRandomUser() throws Exception {
        var mockUser = TestUserGenerator.mockRandomUserResponse();

        when(randomUserService.getRandomUser()).thenReturn(mockUser);

        mockMvc.perform(get("/api/random-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].name.first").value("John"))
                .andExpect(jsonPath("$.results[0].name.last").value("Doe"))
                .andExpect(jsonPath("$.results[0].location.city").value("Springfield"))
                .andExpect(jsonPath("$.results[0].email").value("johndoe@example.com"));
    }
}
