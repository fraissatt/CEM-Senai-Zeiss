package com.zeiss.pilot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maquinas")
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String modelo;
    private String tipoMedida;
    private String volumeMedicao;
    private String fabricante;
    private Integer anoInstalacao;
    private String patrimonioId;
    private String status;
    private boolean ligada = false;
    private String usuarioAtual;

    @Column(length = 1000)
    private String observacao;

    // Raiz de agregação: sessões, manutenções e agendamentos são expostos via
    // endpoints aninhados (/api/maquinas/{id}/sessoes, .../manutencoes, .../agendamentos),
    // não embutidos no JSON da própria Máquina — por isso @JsonIgnore aqui.
    @OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<SessaoMaquina> sessoes = new ArrayList<>();

    @OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ManutencaoMaquina> manutencoes = new ArrayList<>();

    @OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<AgendamentoMaquina> agendamentos = new ArrayList<>();

    public Maquina() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getTipoMedida() { return tipoMedida; }
    public void setTipoMedida(String tipoMedida) { this.tipoMedida = tipoMedida; }
    public String getVolumeMedicao() { return volumeMedicao; }
    public void setVolumeMedicao(String volumeMedicao) { this.volumeMedicao = volumeMedicao; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public Integer getAnoInstalacao() { return anoInstalacao; }
    public void setAnoInstalacao(Integer anoInstalacao) { this.anoInstalacao = anoInstalacao; }
    public String getPatrimonioId() { return patrimonioId; }
    public void setPatrimonioId(String patrimonioId) { this.patrimonioId = patrimonioId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isLigada() { return ligada; }
    public void setLigada(boolean ligada) { this.ligada = ligada; }
    public String getUsuarioAtual() { return usuarioAtual; }
    public void setUsuarioAtual(String usuarioAtual) { this.usuarioAtual = usuarioAtual; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public List<SessaoMaquina> getSessoes() { return sessoes; }
    public void setSessoes(List<SessaoMaquina> sessoes) { this.sessoes = sessoes; }
    public List<ManutencaoMaquina> getManutencoes() { return manutencoes; }
    public void setManutencoes(List<ManutencaoMaquina> manutencoes) { this.manutencoes = manutencoes; }
    public List<AgendamentoMaquina> getAgendamentos() { return agendamentos; }
    public void setAgendamentos(List<AgendamentoMaquina> agendamentos) { this.agendamentos = agendamentos; }

    // Liga a máquina, exceto se estiver em manutenção; abre uma nova sessão de uso
    // na coleção `sessoes` para o usuário atualmente registrado.
    public void ligar() {
        if ("Manutenção".equals(status)) {
            throw new IllegalStateException("Não é possível ligar a máquina: em manutenção.");
        }
        this.ligada = true;
        SessaoMaquina sessao = new SessaoMaquina();
        sessao.setMaquina(this);
        sessao.setUsuario(this.usuarioAtual);
        sessao.setDataLigada(LocalDateTime.now());
        this.sessoes.add(sessao);
    }

    // Desliga a máquina e encerra a sessão de uso em aberto (se houver),
    // calculando as horas de uso decorridas.
    public void desligar() {
        this.ligada = false;
        sessoes.stream()
                .filter(s -> s.getDataDesligada() == null)
                .reduce((primeira, ultima) -> ultima)
                .ifPresent(sessao -> {
                    LocalDateTime agora = LocalDateTime.now();
                    sessao.setDataDesligada(agora);
                    if (sessao.getDataLigada() != null) {
                        sessao.setHorasUso(Duration.between(sessao.getDataLigada(), agora).toMinutes() / 60.0);
                    }
                });
    }
}
