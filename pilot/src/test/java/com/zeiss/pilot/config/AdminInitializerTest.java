package com.zeiss.pilot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zeiss.pilot.entity.Usuario;
import com.zeiss.pilot.repository.UsuarioRepository;

class AdminInitializerTest {

    private static final String EMAIL = "admin.demo@zeiss.com";
    private static final String SENHA = "Senha-De-Teste-123";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminInitializer initializer() {
        return new AdminInitializer(usuarioRepository, passwordEncoder, EMAIL, SENHA);
    }

    @Test
    void initAdmin_semUsuarioComOEmail_criaAdminComSenhaCriptografada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        initializer().initAdmin();

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        Usuario admin = salvo.getValue();
        assertEquals(EMAIL, admin.getEmail());
        assertEquals("ADMIN", admin.getRole());
        assertEquals("DIRETOR_CEM", admin.getCargo());
        assertNotEquals(SENHA, admin.getSenha());
        assertTrue(passwordEncoder.matches(SENHA, admin.getSenha()));
    }

    @Test
    void initAdmin_comUsuarioJaExistente_naoSobrescreveNada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));

        initializer().initAdmin();

        verify(usuarioRepository, never()).save(any());
    }
}
