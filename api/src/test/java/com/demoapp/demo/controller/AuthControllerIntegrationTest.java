package com.demoapp.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do AuthController.
 *
 * Usa H2 em memória (profile "test") — sem dependência de MySQL.
 * Valida o comportamento real dos endpoints de autenticação, incluindo bugs intencionais.
 *
 * Atividade 4 — Testes de Integração de Backend
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueEmail;

    @BeforeEach
    void setUp() {
        uniqueEmail = "user." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    // =========================================================
    // POST /auth/signup — Cadastro
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] POST /auth/signup — dados válidos retorna 200 com id")
    void signup_dadosValidos_retornaOkComId() throws Exception {
        var body = Map.of("email", uniqueEmail, "password", "Forte@123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(uniqueEmail));
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/signup — senha inválida retorna 422")
    void signup_senhaInvalida_retorna422() throws Exception {
        var body = Map.of("email", uniqueEmail, "password", "fraca");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Senha inválida"));
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/signup — email inválido retorna 422")
    void signup_emailInvalido_retorna422() throws Exception {
        var body = Map.of("email", "emailsemarroba", "password", "Forte@123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("E-mail inválido"));
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/signup — email duplicado retorna 409 com mensagem correta")
    void signup_emailDuplicado_retorna409_comMensagemCorreta() throws Exception {
        var body = Map.of("email", uniqueEmail, "password", "Forte@123");
        String json = objectMapper.writeValueAsString(body);

        // Primeiro cadastro — deve funcionar
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // Segundo cadastro com mesmo e-mail
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    // =========================================================
    // POST /auth/signin — Login
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] POST /auth/signin — credenciais corretas retorna 200 com dados do usuário")
    void signin_credenciaisCorretas_retornaOkComDados() throws Exception {
        // Cria o usuário primeiro
        var signupBody = Map.of("email", uniqueEmail, "password", "Forte@123");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupBody)))
                .andExpect(status().isOk());

        // Faz login
        var signinBody = Map.of("email", uniqueEmail, "password", "Forte@123");
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(uniqueEmail));
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/signin — credenciais incorretas retorna 401 com mensagem correta")
    void signin_credenciaisErradas_retorna401() throws Exception {
        var body = Map.of("email", "naoexiste@email.com", "password", "Forte@123");

        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/signin — senha errada retorna 401")
    void signin_senhaErrada_retorna401() throws Exception {
        // Cria usuário
        var signupBody = Map.of("email", uniqueEmail, "password", "Forte@123");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupBody)))
                .andExpect(status().isOk());

        // Tenta login com senha errada
        var signinBody = Map.of("email", uniqueEmail, "password", "SenhaErrada@9");
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinBody)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    // =========================================================
    // POST /auth/reset-password — Reset de senha
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] POST /auth/reset-password — email não cadastrado retorna 404 com mensagem correta")
    void resetPassword_emailNaoCadastrado_retorna404() throws Exception {
        var body = Map.of("email", "naocadastrado." + UUID.randomUUID() + "@email.com");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/reset-password — email cadastrado retorna 200 com mensagem de sucesso")
    void resetPassword_emailCadastrado_retorna200() throws Exception {
        // Cria usuário primeiro
        var signupBody = Map.of("email", uniqueEmail, "password", "Forte@123");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupBody)))
                .andExpect(status().isOk());

        // Reset de senha
        var resetBody = Map.of("email", uniqueEmail);
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("[SUCESSO] POST /auth/reset-password — email inválido retorna 422")
    void resetPassword_emailInvalido_retorna422() throws Exception {
        var body = Map.of("email", "emailinvalido");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }
}
