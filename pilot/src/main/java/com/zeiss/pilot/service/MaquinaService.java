package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.AgendamentoMaquina;
import com.zeiss.pilot.entity.Maquina;
import com.zeiss.pilot.entity.ManutencaoMaquina;
import com.zeiss.pilot.entity.SessaoMaquina;
import com.zeiss.pilot.repository.AgendamentoMaquinaRepository;
import com.zeiss.pilot.repository.MaquinaRepository;
import com.zeiss.pilot.repository.ManutencaoMaquinaRepository;
import com.zeiss.pilot.repository.SessaoMaquinaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final SessaoMaquinaRepository sessaoRepository;
    private final ManutencaoMaquinaRepository manutencaoRepository;
    private final AgendamentoMaquinaRepository agendamentoRepository;

    public MaquinaService(MaquinaRepository maquinaRepository, SessaoMaquinaRepository sessaoRepository, ManutencaoMaquinaRepository manutencaoRepository, AgendamentoMaquinaRepository agendamentoRepository) {
        this.maquinaRepository = maquinaRepository;
        this.sessaoRepository = sessaoRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    public List<Maquina> listar() {
        return maquinaRepository.findAll();
    }

    public Maquina buscarPorId(Long id) {
        return maquinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));
    }

    public Maquina salvar(Maquina dados) {
        Maquina maquina = new Maquina();
        copiarCampos(dados, maquina);
        return maquinaRepository.save(maquina);
    }

    public Maquina atualizar(Long id, Maquina dados) {
        Maquina maquina = maquinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));
        copiarCampos(dados, maquina);
        return maquinaRepository.save(maquina);
    }

    private void copiarCampos(Maquina origem, Maquina destino) {
        destino.setNome(origem.getNome());
        destino.setModelo(origem.getModelo());
        destino.setTipoMedida(origem.getTipoMedida());
        destino.setVolumeMedicao(origem.getVolumeMedicao());
        destino.setFabricante(origem.getFabricante());
        destino.setAnoInstalacao(origem.getAnoInstalacao());
        destino.setPatrimonioId(origem.getPatrimonioId());
        destino.setStatus(origem.getStatus());
        destino.setLigada(origem.isLigada());
        destino.setUsuarioAtual(origem.getUsuarioAtual());
        destino.setObservacao(origem.getObservacao());
    }

    public void deletar(Long id) {
        maquinaRepository.deleteById(id);
    }

    // Sessões

    public List<SessaoMaquina> listarSessoes(Long maquinaId) {
        return sessaoRepository.findByMaquinaId(maquinaId);
    }

    public SessaoMaquina criarSessao(Long maquinaId, SessaoMaquina dados) {
        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + maquinaId));
        SessaoMaquina s = new SessaoMaquina();
        s.setMaquina(maquina);
        s.setUsuario(dados.getUsuario());
        s.setDataLigada(dados.getDataLigada());
        s.setDataDesligada(dados.getDataDesligada());
        s.setHorasUso(dados.getHorasUso());
        s.setMotivo(dados.getMotivo());
        s.setObservacao(dados.getObservacao());
        return sessaoRepository.save(s);
    }

    public SessaoMaquina atualizarSessao(Long id, SessaoMaquina dados) {
        SessaoMaquina existing = sessaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada: " + id));
        existing.setUsuario(dados.getUsuario());
        existing.setDataLigada(dados.getDataLigada());
        existing.setDataDesligada(dados.getDataDesligada());
        existing.setHorasUso(dados.getHorasUso());
        existing.setMotivo(dados.getMotivo());
        existing.setObservacao(dados.getObservacao());
        return sessaoRepository.save(existing);
    }

    public void deletarSessao(Long id) {
        sessaoRepository.deleteById(id);
    }

    // Manutenções

    public List<ManutencaoMaquina> listarManutencoes(Long maquinaId) {
        return manutencaoRepository.findByMaquinaId(maquinaId);
    }

    public ManutencaoMaquina criarManutencao(Long maquinaId, ManutencaoMaquina dados) {
        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + maquinaId));
        ManutencaoMaquina m = new ManutencaoMaquina();
        m.setMaquina(maquina);
        m.setTipo(dados.getTipo());
        m.setResponsavel(dados.getResponsavel());
        m.setData(dados.getData());
        m.setProximaData(dados.getProximaData());
        m.setStatus(dados.getStatus());
        m.setObservacao(dados.getObservacao());
        return manutencaoRepository.save(m);
    }

    public ManutencaoMaquina atualizarManutencao(Long id, ManutencaoMaquina dados) {
        ManutencaoMaquina existing = manutencaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manutenção não encontrada: " + id));
        existing.setTipo(dados.getTipo());
        existing.setResponsavel(dados.getResponsavel());
        existing.setData(dados.getData());
        existing.setProximaData(dados.getProximaData());
        existing.setStatus(dados.getStatus());
        existing.setObservacao(dados.getObservacao());
        return manutencaoRepository.save(existing);
    }

    public void deletarManutencao(Long id) {
        manutencaoRepository.deleteById(id);
    }

    // Agendamentos

    public List<AgendamentoMaquina> listarAgendamentos(Long maquinaId) {
        return agendamentoRepository.findByMaquinaId(maquinaId);
    }

    public AgendamentoMaquina criarAgendamento(Long maquinaId, AgendamentoMaquina dados) {
        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + maquinaId));
        AgendamentoMaquina a = new AgendamentoMaquina();
        a.setMaquina(maquina);
        a.setUsuario(dados.getUsuario());
        a.setDataInicio(dados.getDataInicio());
        a.setDataFim(dados.getDataFim());
        a.setMotivo(dados.getMotivo());
        a.setConfirmado(dados.isConfirmado());
        return agendamentoRepository.save(a);
    }

    public AgendamentoMaquina atualizarAgendamento(Long id, AgendamentoMaquina dados) {
        AgendamentoMaquina existing = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado: " + id));
        existing.setUsuario(dados.getUsuario());
        existing.setDataInicio(dados.getDataInicio());
        existing.setDataFim(dados.getDataFim());
        existing.setMotivo(dados.getMotivo());
        existing.setConfirmado(dados.isConfirmado());
        return agendamentoRepository.save(existing);
    }

    public void deletarAgendamento(Long id) {
        agendamentoRepository.deleteById(id);
    }
}
