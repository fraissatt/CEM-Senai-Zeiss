package com.zeiss.pilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zeiss.pilot.entity.Usuario;
import com.zeiss.pilot.repository.UsuarioRepository;

import jakarta.annotation.PostConstruct;

@Configuration
public class AdminInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminSenha;

    public AdminInitializer(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email:admin.demo@zeiss.com}") String adminEmail,
                            @Value("${app.admin.password:Zeiss@Demo#2026}") String adminSenha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
    }

    @PostConstruct
    public void initAdmin() {
        if (usuarioRepository.findByEmail(adminEmail).isPresent()) return;

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(adminEmail);
        admin.setCargo("DIRETOR_CEM");
        admin.derivarRoleDoCargo();
        admin.setSenha(passwordEncoder.encode(adminSenha));
        usuarioRepository.save(admin);

        System.out.println("[INIT] Usuário ADMIN criado: " + adminEmail);
    }
}
