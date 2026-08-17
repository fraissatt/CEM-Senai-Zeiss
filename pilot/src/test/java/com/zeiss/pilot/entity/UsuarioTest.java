package com.zeiss.pilot.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void derivarRoleDoCargo_semCargo_viraEstagiario() {
        Usuario usuario = new Usuario();
        usuario.setCargo(null);

        usuario.derivarRoleDoCargo();

        assertEquals("ESTAGIARIO", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_estagiario_viraEstagiario() {
        Usuario usuario = new Usuario();
        usuario.setCargo("estagiario");

        usuario.derivarRoleDoCargo();

        assertEquals("ESTAGIARIO", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_gestor_viraTecnico() {
        Usuario usuario = new Usuario();
        usuario.setCargo("GESTOR");

        usuario.derivarRoleDoCargo();

        assertEquals("TECNICO", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_diretorCem_viraAdmin() {
        Usuario usuario = new Usuario();
        usuario.setCargo("DIRETOR_CEM");

        usuario.derivarRoleDoCargo();

        assertEquals("ADMIN", usuario.getRole());
    }

    @Test
    void validarSenhaObrigatoria_senhaValida_naoLanca() {
        Usuario usuario = new Usuario();
        usuario.setSenha("segredo123");

        assertDoesNotThrow(usuario::validarSenhaObrigatoria);
    }

    @Test
    void validarSenhaObrigatoria_senhaNula_lanca() {
        Usuario usuario = new Usuario();
        usuario.setSenha(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, usuario::validarSenhaObrigatoria);
        assertEquals("Senha obrigatória para criar usuário", ex.getMessage());
    }

    @Test
    void validarSenhaObrigatoria_senhaEmBranco_lanca() {
        Usuario usuario = new Usuario();
        usuario.setSenha("   ");

        assertThrows(IllegalArgumentException.class, usuario::validarSenhaObrigatoria);
    }
}
