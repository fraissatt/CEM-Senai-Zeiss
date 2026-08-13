package com.zeiss.pilot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zeiss.pilot.entity.Edital;
import com.zeiss.pilot.repository.EditalRepository;

@Service
public class EditalService {

    private final EditalRepository editalRepository;

    public EditalService(EditalRepository editalRepository) {
        this.editalRepository = editalRepository;
    }

    public List<Edital> listarTodos() {
        return editalRepository.findAll();
    }

    public Edital buscarPorId(Long id) {
        return editalRepository.findById(id).orElse(null);
    }

    public Edital criarEdital(Edital edital) {
        return editalRepository.save(edital);
    }

    public Edital atualizarEdital(Long id, Edital edital) {
        return editalRepository.findById(id)
                .map(editalExistente -> {
                    editalExistente.setNomeEdital(edital.getNomeEdital());
                    editalExistente.setInstituicaoFornecedora(edital.getInstituicaoFornecedora());
                    editalExistente.setInstituicaoParceira(edital.getInstituicaoParceira());
                    editalExistente.setStatus(edital.getStatus());
                    editalExistente.setValor(edital.getValor());
                    editalExistente.setObservacao(edital.getObservacao());
                    return editalRepository.save(editalExistente);
                })
                .orElseThrow(() -> new RuntimeException("Edital não encontrado com ID: " + id));
    }

    public void excluirEdital(Long id) {
        if (!editalRepository.existsById(id)) {
            throw new RuntimeException("Edital não encontrado com ID: " + id);
        }
        editalRepository.deleteById(id);
    }
}
