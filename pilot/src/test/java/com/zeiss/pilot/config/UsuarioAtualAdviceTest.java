package com.zeiss.pilot.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.zeiss.pilot.entity.Usuario;
import com.zeiss.pilot.repository.UsuarioRepository;

class UsuarioAtualAdviceTest {

    @AfterEach
    void limparSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usuarioAutenticado_injetaNomeERole() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setNome("Administrador");
        usuario.setRole("ADMIN");
        when(repo.findByEmail("admin@zeiss.com")).thenReturn(Optional.of(usuario));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@zeiss.com", "N/A", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

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

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "fantasma@zeiss.com", "N/A", List.of(new SimpleGrantedAuthority("ROLE_ESTAGIARIO")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Model model = new ConcurrentModel();
        Principal principal = () -> "fantasma@zeiss.com";

        assertDoesNotThrow(() -> new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(principal, model));

        assertFalse(model.containsAttribute("usuarioNome"));
        assertFalse(model.containsAttribute("usuarioRole"));
    }

    @Test
    void authenticationAnonima_naoLancaENaoInjeta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);

        AnonymousAuthenticationToken anonimo = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonimo);

        Model model = new ConcurrentModel();

        // Em producao o Principal chega null pra requisicoes anonimas (o wrapper
        // do Spring Security nao expoe AnonymousAuthenticationToken como
        // getUserPrincipal()). Aqui simulamos diretamente contra a Authentication
        // do SecurityContextHolder, que e' o dado que o metodo de fato usa pra
        // extrair a role: mesmo com um Principal presente, so ROLE_ANONYMOUS
        // disponivel nao deve injetar nada.
        assertDoesNotThrow(() -> new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(anonimo, model));

        assertFalse(model.containsAttribute("usuarioNome"));
        assertFalse(model.containsAttribute("usuarioRole"));
    }
}
