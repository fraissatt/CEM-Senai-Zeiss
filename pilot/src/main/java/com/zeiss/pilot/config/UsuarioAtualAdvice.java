package com.zeiss.pilot.config;

import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zeiss.pilot.controller.PageController;
import com.zeiss.pilot.repository.UsuarioRepository;

// Injeta nome e role do usuario autenticado no Model de toda pagina renderizada
// pelo PageController, para que o Auth do core.js leia esses dados direto do HTML
// (sem fetch assincrono, sem estado intermediario onde o app nao sabe quem esta logado).
//
// A role vem da Authentication (SecurityContextHolder), nao do banco: e' o valor
// congelado na sessao no login (CustomUserDetailsService) que o Spring Security de
// fato usa pra autorizar cada requisicao. Se fosse relida do banco aqui, um usuario
// promovido/rebaixado em runtime (PUT /api/usuarios/{id}) veria a role renderizada
// na pagina divergir da role realmente aplicada pelo servidor ate o proximo login.
//
// Escopado a PageController de proposito: sem assignableTypes, este advice rodaria
// tambem em toda requisicao de @RestController, disparando um findByEmail por
// chamada de API para um valor que as respostas JSON nunca usam.
@ControllerAdvice(assignableTypes = PageController.class)
public class UsuarioAtualAdvice {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    private final UsuarioRepository usuarioRepository;

    public UsuarioAtualAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void adicionarUsuarioAtual(Principal principal, Model model) {
        // Principal nulo = usuario anonimo (ex.: /avaliacao, que e' permitAll e
        // carrega core.js) — o wrapper do Spring Security ja devolve null pra
        // AnonymousAuthenticationToken. Role nao encontrada (ex.: so
        // ROLE_ANONYMOUS presente) e usuario ausente no banco (sessao viva de
        // usuario deletado) tem o mesmo desfecho: nao injeta nada e o Auth cai
        // no menor privilegio (ESTAGIARIO) — nunca escala privilegio por falta
        // de dado.
        if (principal == null) {
            return;
        }

        String role = extrairRole(SecurityContextHolder.getContext().getAuthentication());
        if (role == null) {
            return;
        }

        usuarioRepository.findByEmail(principal.getName()).ifPresent(usuario -> {
            model.addAttribute("usuarioNome", usuario.getNome());
            model.addAttribute("usuarioRole", role);
        });
    }

    // Authorities sao prefixadas "ROLE_" (ex.: ROLE_ADMIN); templates e core.js
    // esperam a role "nua" (ADMIN). Um usuario carrega exatamente uma role
    // (CustomUserDetailsService concede um unico SimpleGrantedAuthority), entao
    // pega a primeira authority "ROLE_*" que nao seja o marcador de anonimo.
    private String extrairRole(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String nome = authority.getAuthority();
            if (nome.startsWith(ROLE_PREFIX) && !nome.equals(ROLE_ANONYMOUS)) {
                return nome.substring(ROLE_PREFIX.length());
            }
        }
        return null;
    }
}
