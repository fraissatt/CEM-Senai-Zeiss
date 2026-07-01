package com.zeiss.pilot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.zeiss.pilot.entity.Projeto;
import com.zeiss.pilot.repository.ProjetoRepository;
import com.zeiss.pilot.repository.UsuarioRepository;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProjetoService(ProjetoRepository projetoRepository, UsuarioRepository usuarioRepository) {
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Projeto criarProjeto(Projeto dados) {
        Projeto projeto = new Projeto();
        projeto.setNomeProjeto(dados.getNomeProjeto());
        projeto.setObjetivo(dados.getObjetivo());
        projeto.setAtividades(dados.getAtividades());
        if (dados.getResponsavel() != null && dados.getResponsavel().getId() != null) {
            usuarioRepository.findById(dados.getResponsavel().getId()).ifPresent(projeto::setResponsavel);
        }
        projeto.setPrioridade(dados.getPrioridade());
        projeto.setCustoAnualPrevisto(dados.getCustoAnualPrevisto());
        projeto.setRetornoPrevisto(dados.getRetornoPrevisto());
        projeto.setStatus(dados.getStatus());
        projeto.setObservacao(dados.getObservacao());
        projeto.setPrevisaoInicio(dados.getPrevisaoInicio());
        projeto.setPrevisaoTermino(dados.getPrevisaoTermino());
        projeto.setDataRealFinalizacao(dados.getDataRealFinalizacao());
        return projetoRepository.save(projeto);
    }

    public List<Projeto> listarProjetos() {
        return projetoRepository.findAll();
    }

    public Optional<Projeto> buscarPorId(Long id) {
        return projetoRepository.findById(id);
    }

    public boolean excluirProjeto(Long id) {
        if (projetoRepository.existsById(id)) {
            projetoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Projeto atualizarProjeto(Long id, Projeto dados) {
        return projetoRepository.findById(id).map(projeto -> {
            projeto.setNomeProjeto(dados.getNomeProjeto());
            projeto.setObjetivo(dados.getObjetivo());
            projeto.setAtividades(dados.getAtividades());
            if (dados.getResponsavel() != null && dados.getResponsavel().getId() != null) {
                usuarioRepository.findById(dados.getResponsavel().getId()).ifPresent(projeto::setResponsavel);
            } else {
                projeto.setResponsavel(null);
            }
            projeto.setPrioridade(dados.getPrioridade());
            projeto.setCustoAnualPrevisto(dados.getCustoAnualPrevisto());
            projeto.setRetornoPrevisto(dados.getRetornoPrevisto());
            projeto.setStatus(dados.getStatus());
            projeto.setObservacao(dados.getObservacao());
            projeto.setPrevisaoInicio(dados.getPrevisaoInicio());
            projeto.setPrevisaoTermino(dados.getPrevisaoTermino());
            projeto.setDataRealFinalizacao(dados.getDataRealFinalizacao());
            return projetoRepository.save(projeto);
        }).orElse(null);
    }
}
