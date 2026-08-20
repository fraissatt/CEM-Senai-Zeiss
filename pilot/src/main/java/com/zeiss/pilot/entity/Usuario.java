package com.zeiss.pilot.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(nullable = false, length = 50)
    private String role = "CLIENTE";

    @Column(length = 50)
    private String cargo;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao = LocalDateTime.now();

    // Permissões individuais (aba "Permissões" na Gerência de Usuários). Nulas = ainda não
    // customizadas para este usuário; o front aplica os padrões do cargo nesse caso.
    @Column(name = "perm_create")
    private Boolean create;

    @Column(name = "perm_edit")
    private Boolean edit;

    @Column(name = "perm_delete")
    private Boolean delete;

    @Column(name = "perm_view_editais")
    private Boolean viewEditais;

    @Column(name = "perm_view_documentos")
    private Boolean viewDocumentos;

    @Column(name = "perm_view_usuarios")
    private Boolean viewUsuarios;

    @Column(name = "perm_view_financial")
    private Boolean viewFinancial;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // WRITE_ONLY: nunca serializar a senha em respostas JSON (ex.: Usuario aparece
    // aninhado em Projeto.responsavel e outras relacoes agora que a Entity e' retornada direto)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public Boolean getCreate() { return create; }
    public void setCreate(Boolean create) { this.create = create; }

    public Boolean getEdit() { return edit; }
    public void setEdit(Boolean edit) { this.edit = edit; }

    public Boolean getDelete() { return delete; }
    public void setDelete(Boolean delete) { this.delete = delete; }

    public Boolean getViewEditais() { return viewEditais; }
    public void setViewEditais(Boolean viewEditais) { this.viewEditais = viewEditais; }

    public Boolean getViewDocumentos() { return viewDocumentos; }
    public void setViewDocumentos(Boolean viewDocumentos) { this.viewDocumentos = viewDocumentos; }

    public Boolean getViewUsuarios() { return viewUsuarios; }
    public void setViewUsuarios(Boolean viewUsuarios) { this.viewUsuarios = viewUsuarios; }

    public Boolean getViewFinancial() { return viewFinancial; }
    public void setViewFinancial(Boolean viewFinancial) { this.viewFinancial = viewFinancial; }

    // Deriva o role de segurança a partir do cargo organizacional: ESTAGIARIO (ou sem cargo) = CLIENTE, qualquer outro = ADMIN.
    public void derivarRoleDoCargo() {
        if (cargo != null && !cargo.equalsIgnoreCase("ESTAGIARIO")) {
            this.role = "ADMIN";
        } else {
            this.role = "CLIENTE";
        }
    }

    public void validarSenhaObrigatoria() {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória para criar usuário");
        }
    }
}
