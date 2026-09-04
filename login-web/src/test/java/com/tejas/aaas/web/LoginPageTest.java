package com.tejas.aaas.web;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
@SpringBootTest @AutoConfigureMockMvc
class LoginPageTest {
    @Autowired MockMvc mvc;
    @Test void localLoginScreenRendersWithoutAws() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Welcome back.")))
            .andExpect(content().string(containsString("Sign-in is not configured yet")));
    }
    @Test void localPreviewCannotAccessAccounts() throws Exception { mvc.perform(get("/account")).andExpect(status().isForbidden()); }
    @Test void registrationRequiresCsrf() throws Exception { mvc.perform(post("/registration")).andExpect(status().isForbidden()); }
}
