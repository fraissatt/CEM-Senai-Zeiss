package com.zeiss.pilot.dto;

public class PastaDocumentoCriarRequest {
    private String nome;
    private String tipoAcesso;
    private Long pastaPaiId;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipoAcesso() { return tipoAcesso; }
    public void setTipoAcesso(String tipoAcesso) { this.tipoAcesso = tipoAcesso; }

    public Long getPastaPaiId() { return pastaPaiId; }
    public void setPastaPaiId(Long pastaPaiId) { this.pastaPaiId = pastaPaiId; }
}
