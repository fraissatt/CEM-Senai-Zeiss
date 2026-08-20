package com.zeiss.pilot.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Estagiário (role de segurança CLIENTE) só tem acesso às telas Kanban de Atividades
     * e Dashboard Estagiários + as APIs que essas duas telas consomem. Todo o resto exige ADMIN
     * (Gestor e Diretor, hoje mapeados para o mesmo ROLE_ADMIN — distinção entre eles fica para depois).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/css/**", "/img/**", "/js/**",
                    "/avaliacao", "/qrcode-avaliacao", "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/avaliacoes").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/eventos").hasRole("ADMIN")
                // Criar tarefa no Kanban é trabalho do Gestor/Diretor, não do Estagiário
                .requestMatchers(HttpMethod.POST, "/api/kanban-cards/**").hasRole("ADMIN")

                // Telas liberadas para Estagiário
                .requestMatchers(HttpMethod.GET, "/kanban-estagiarios", "/dashboard-estagiarios").authenticated()
                .requestMatchers("/api/kanban-cards/**", "/api/dashboard-estagiarios/**", "/api/notas-estagiarios/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/estagiarios/**", "/api/servicos/**").authenticated()

                // Demais páginas e endpoints exigem ADMIN (bloqueia Estagiário)
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) ->
                response.sendRedirect(isAdmin(authentication) ? "/index" : "/kanban-estagiarios");
    }

    /**
     * Requisições de página (não-API) barradas por falta de role viram redirect
     * para uma tela permitida, em vez do whitelabel 403. Chamadas de API continuam
     * devolvendo 403 puro, como o front já espera.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            boolean admin = isAdmin(SecurityContextHolder.getContext().getAuthentication());
            response.sendRedirect(admin ? "/index" : "/kanban-estagiarios");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
