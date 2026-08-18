package com.zeiss.pilot.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.zeiss.pilot.entity.Usuario;
import com.zeiss.pilot.repository.UsuarioRepository;

class UsuarioAtualAdviceTest {

    @Test
    void usuarioAutenticado_injetaNomeERole() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setNome("Administrador");
        usuario.setRole("ADMIN");
        when(repo.findByEmail("admin@zeiss.com")).thenReturn(Optional.of(usuario));

        Model model = new ConcurrentModel();
        Principal principal = () -> "admin@zeiss.com";

        new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(principal, model);

        assertEquals("Administrador", model.getAttribute("usuarioNome"));
        assertEquals("ADMIN", model.getAttribute("usuarioRole"));
    }

    @Test
    void principalNulo_naoLancaENaoInjeta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Model model = new ConcurrentModel();

        assertDoesNotThrow(() -> new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(null, model));

        assertFalse(model.containsAttribute("usuarioNome"));
        assertFalse(model.containsAttribute("usuarioRole"));
    }

    @Test
    void usuarioAusenteNoBanco_naoLancaENaoInjeta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.findByEmail(anyString())).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        Principal principal = () -> "fantasma@zeiss.com";

        assertDoesNotThrow(() -> new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(principal, model));

        assertFalse(model.containsAttribute("usuarioNome"));
        assertFalse(model.containsAttribute("usuarioRole"));
    }
}
