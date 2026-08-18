package com.zeiss.pilot.config;

import java.security.Principal;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zeiss.pilot.controller.PageController;
import com.zeiss.pilot.repository.UsuarioRepository;

// Injeta nome e role do usuario autenticado no Model de toda pagina renderizada
// pelo PageController, para que o Auth do core.js leia esses dados direto do HTML
// (sem fetch assincrono, sem estado intermediario onde o app nao sabe quem esta logado).
//
// Escopado a PageController de proposito: sem assignableTypes, este advice rodaria
// tambem em toda requisicao de @RestController, disparando um findByEmail por
// chamada de API para um valor que as respostas JSON nunca usam.
@ControllerAdvice(assignableTypes = PageController.class)
public class UsuarioAtualAdvice {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAtualAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void adicionarUsuarioAtual(Principal principal, Model model) {
        // Principal nulo = usuario anonimo (ex.: /avaliacao, que e' permitAll e
        // carrega core.js). Usuario ausente no banco = sessao viva de usuario
        // deletado. Nos dois casos nao injeta nada e o Auth cai no menor
        // privilegio (ESTAGIARIO) — nunca escala privilegio por falta de dado.
        if (principal == null) {
            return;
        }
        usuarioRepository.findByEmail(principal.getName()).ifPresent(usuario -> {
            model.addAttribute("usuarioNome", usuario.getNome());
            model.addAttribute("usuarioRole", usuario.getRole());
        });
    }
}
