package com.zeiss.pilot.dto;

public class PastaDocumentoAtualizarRequest {
    private String nome;
    private String tipoAcesso;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipoAcesso() { return tipoAcesso; }
    public void setTipoAcesso(String tipoAcesso) { this.tipoAcesso = tipoAcesso; }
}
