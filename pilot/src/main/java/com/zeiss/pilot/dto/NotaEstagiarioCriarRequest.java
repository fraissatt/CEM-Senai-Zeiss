package com.zeiss.pilot.dto;

import java.time.LocalDate;

public class NotaEstagiarioCriarRequest {
    private Long estagiariaId;
    private Long servicoId;
    private int nota;
    private String comentario;
    private String avaliadorNome;
    private LocalDate data;

    public Long getEstagiariaId() { return estagiariaId; }
    public void setEstagiariaId(Long estagiariaId) { this.estagiariaId = estagiariaId; }

    public Long getServicoId() { return servicoId; }
    public void setServicoId(Long servicoId) { this.servicoId = servicoId; }

    public int getNota() { return nota; }
    public void setNota(int nota) { this.nota = nota; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public String getAvaliadorNome() { return avaliadorNome; }
    public void setAvaliadorNome(String avaliadorNome) { this.avaliadorNome = avaliadorNome; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
