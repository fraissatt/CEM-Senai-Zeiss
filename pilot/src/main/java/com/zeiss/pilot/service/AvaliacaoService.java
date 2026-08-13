package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.Avaliacao;
import com.zeiss.pilot.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public List<Avaliacao> listar() {
        return repository.findAll();
    }

    public Avaliacao salvar(Avaliacao entity) {
        entity.setId(null);
        return repository.save(entity);
    }
}
