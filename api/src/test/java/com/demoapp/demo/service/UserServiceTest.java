package com.demoapp.demo.service;

import com.demoapp.demo.model.User;
import com.demoapp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do UserService.
 * Usa Mockito para isolar o repositório — sem banco de dados real.
 *
 * Atividade 4 — Testes Unitários de Backend
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("joao.teste@email.com");
        existingUser.setPassword("Senha@123");
    }

    // =========================================================
    // TESTES DE isPasswordValid — Validação de senha
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] isPasswordValid — senha forte retorna true")
    void isPasswordValid_senhaForte_deveRetornarTrue() {
        // Senha com todos os critérios: 8+ chars, maiúscula, minúscula, número, especial
        assertTrue(userService.isPasswordValid("Forte@123"),
                "Senha forte 'Forte@123' deve ser considerada válida");
    }

    @Test
    @DisplayName("[SUCESSO] isPasswordValid — senha sem caractere especial retorna false")
    void isPasswordValid_senhaSemEspecial_deveRetornarFalse() {
        assertFalse(userService.isPasswordValid("SemEspecial1"),
                "Senha sem caractere especial deve ser inválida");
    }

    @Test
    @DisplayName("[SUCESSO] isPasswordValid — senha sem letra maiúscula retorna false")
    void isPasswordValid_senhaSemMaiuscula_deveRetornarFalse() {
        assertFalse(userService.isPasswordValid("semmaius@1"),
                "Senha sem letra maiúscula deve ser inválida");
    }

    @Test
    @DisplayName("[SUCESSO] isPasswordValid — senha sem número retorna false")
    void isPasswordValid_senhaSemNumero_deveRetornarFalse() {
        assertFalse(userService.isPasswordValid("SemNumero@abc"),
                "Senha sem número deve ser inválida");
    }

    @Test
    @DisplayName("[SUCESSO] isPasswordValid — senha com menos de 8 caracteres retorna false")
    void isPasswordValid_senhaCurta_deveRetornarFalse() {
        assertFalse(userService.isPasswordValid("Ab@1234"),
                "Senha com menos de 8 caracteres deve ser inválida");
    }

    @Test
    @DisplayName("[SUCESSO] isPasswordValid — senha nula retorna false")
    void isPasswordValid_senhaNula_deveRetornarFalse() {
        assertFalse(userService.isPasswordValid(null),
                "Senha nula deve ser inválida");
    }

    // =========================================================
    // TESTES DE isEmailValid — Validação de email
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] isEmailValid — email com @ retorna true")
    void isEmailValid_emailComArroba_deveRetornarTrue() {
        assertTrue(userService.isEmailValid("usuario@dominio.com"),
                "Email com @ deve ser considerado válido");
    }

    @Test
    @DisplayName("[SUCESSO] isEmailValid — email sem @ retorna false")
    void isEmailValid_emailSemArroba_deveRetornarFalse() {
        assertFalse(userService.isEmailValid("emailsemarroba.com"),
                "Email sem @ deve ser inválido");
    }

    @Test
    @DisplayName("[SUCESSO] isEmailValid — email nulo retorna false")
    void isEmailValid_emailNulo_deveRetornarFalse() {
        assertFalse(userService.isEmailValid(null),
                "Email nulo deve ser inválido");
    }

    // =========================================================
    // TESTES DE createUser
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] createUser — salva usuário e retorna com id")
    void createUser_deveAlvaSalvarERetornarUsuario() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        User result = userService.createUser("novo@email.com", "Senha@123");

        assertNotNull(result, "Usuário criado não deve ser nulo");
        assertEquals("novo@email.com", result.getEmail());
        assertEquals("Senha@123", result.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // =========================================================
    // TESTES DE findByEmail
    // =========================================================

    @Test
    @DisplayName("[SUCESSO] findByEmail — email existente retorna usuário")
    void findByEmail_emailExistente_deveRetornarUsuario() {
        when(userRepository.findByEmail("joao.teste@email.com"))
                .thenReturn(Optional.of(existingUser));

        User result = userService.findByEmail("joao.teste@email.com");

        assertNotNull(result, "Deve retornar usuário quando e-mail existe");
        assertEquals("joao.teste@email.com", result.getEmail());
    }

    @Test
    @DisplayName("[SUCESSO] findByEmail — email inexistente retorna null")
    void findByEmail_emailInexistente_deveRetornarNull() {
        when(userRepository.findByEmail("naoexiste@email.com"))
                .thenReturn(Optional.empty());

        User result = userService.findByEmail("naoexiste@email.com");

        assertNull(result, "Deve retornar null quando e-mail não existe");
    }

    // =========================================================
    // TESTE DE BUG INTENCIONAL
    // =========================================================

    @Test
    @DisplayName("[BUG] isPasswordValid — senha com exatamente 8 chars deveria ser válida mas regex usa {8,}")
    void isPasswordValid_senhaComExatamente8Chars_deveSerValida() {
        /*
         * ANÁLISE: O regex do backend usa (?=.*).{8,} que aceita 8+ caracteres.
         * Portanto "Ab@12345" (8 chars) DEVE ser aceita — este teste documenta o comportamento.
         * Se o backend um dia mudar para > 8 chars este teste capturará a regressão.
         */
        boolean result = userService.isPasswordValid("Ab@12345"); // exatamente 8 chars
        assertTrue(result,
                "Senha com exatamente 8 chars deve ser válida (regex usa {8,})." +
                " ATENÇÃO: Frontend usa '<= 8' que rejeita senhas de 8 chars — inconsistência detectada!");
    }
}
