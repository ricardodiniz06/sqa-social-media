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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do PostController.
 * Valida a listagem de posts e as reações (like/dislike).
 *
 * Atividade 4 e 6 (Nova Feature)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createUserAndGetId(String email) throws Exception {
        var signupBody = Map.of("email", email, "password", "Forte@123");
        var result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupBody)))
                .andReturn();
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        return responseBody.get("id").asLong();
    }

    // =========================================================
    // GET /posts — Feed de Posts
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] GET /posts — retorna lista de posts com status 200")
    void getPosts_deveRetornarListaComStatus200() throws Exception {
        String email = "posts." + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        Long userId = createUserAndGetId(email);

        mockMvc.perform(get("/posts")
                        .param("userId", String.valueOf(userId))
                        .param("limit", "5")
                        .param("skip", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", isA(java.util.List.class)))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.posts[0].liked").isBoolean())
                .andExpect(jsonPath("$.posts[0].disliked").isBoolean());
    }

    // =========================================================
    // POST /posts/{postId}/like — Curtir Post
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] POST /posts/{postId}/like — curtir post existente retorna sucesso")
    void likePost_postExistente_retornaSucesso() throws Exception {
        String email = "like." + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        Long userId = createUserAndGetId(email);

        mockMvc.perform(post("/posts/1/like")
                        .param("userId", String.valueOf(userId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.disliked").value(false));
    }

    // =========================================================
    // GET /posts/liked — Posts Curtidos
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] GET /posts/liked — retorna posts curtidos pelo usuário")
    void getLikedPosts_deveRetornarPostsCurtidos() throws Exception {
        String email = "liked." + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        Long userId = createUserAndGetId(email);

        // Curtir um post primeiro
        mockMvc.perform(post("/posts/1/like")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk());

        // Buscar posts curtidos
        mockMvc.perform(get("/posts/liked")
                        .param("userId", String.valueOf(userId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", isA(java.util.List.class)))
                .andExpect(jsonPath("$.posts.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.posts[0].liked").value(true))
                .andExpect(jsonPath("$.posts[0].disliked").value(false));
    }

    // =========================================================
    // POST /posts/{postId}/dislike — Nova Feature (Atividade 6)
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] POST /posts/{postId}/dislike — dar dislike no post retorna sucesso")
    void dislikePost_postExistente_retornaSucesso() throws Exception {
        String email = "dislike." + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        Long userId = createUserAndGetId(email);

        mockMvc.perform(post("/posts/1/dislike")
                        .param("userId", String.valueOf(userId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.disliked").value(true));
    }

    @Test
    @DisplayName("[SUCESSO] Like e Dislike são mutuamente exclusivos")
    void likeAndDislike_mutuamenteExclusivos() throws Exception {
        String email = "mutual." + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        Long userId = createUserAndGetId(email);

        // 1. Dá Like
        mockMvc.perform(post("/posts/2/like")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        // 2. Dá Dislike (deve remover o like)
        mockMvc.perform(post("/posts/2/dislike")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.disliked").value(true));

        // 3. Dá Like de novo (deve remover o dislike)
        mockMvc.perform(post("/posts/2/like")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.disliked").value(false));
    }

}
