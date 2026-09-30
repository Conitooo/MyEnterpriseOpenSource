package com.myenterpriseos.myenterpriseopensource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.registration.code=test-registration-code-2026-secret")
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTests {
    @Autowired MockMvc mvc;

    @Test
    void registrationRequiresCodeAndCreatesUsableAdmin() throws Exception {
        String body = "{\"companyName\":\"New Co\",\"username\":\"owner\","
                + "\"password\":\"ExamplePassword2026!\",\"registrationCode\":\"%s\"}";
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(body.formatted("wrong-code"))).andExpect(status().isForbidden());
        String response = mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(body.formatted("test-registration-code-2026-secret")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("owner"))
                .andReturn().getResponse().getContentAsString();
        long companyId = Long.parseLong(response.replaceAll(".*\\\"companyId\\\":(\\d+).*", "$1"));
        assertTrue(companyId > 0);
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"companyId\":" + companyId + ",\"username\":\"owner\","
                        + "\"password\":\"ExamplePassword2026!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
