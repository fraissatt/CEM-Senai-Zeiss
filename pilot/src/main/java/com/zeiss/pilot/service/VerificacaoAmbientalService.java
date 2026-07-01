package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.VerificacaoAmbiental;
import com.zeiss.pilot.repository.VerificacaoAmbientalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VerificacaoAmbientalService {

    private final VerificacaoAmbientalRepository repository;

    public VerificacaoAmbientalService(VerificacaoAmbientalRepository repository) {
        this.repository = repository;
    }

    public List<VerificacaoAmbiental> listar() {
        return repository.findAll();
    }

    public VerificacaoAmbiental salvar(VerificacaoAmbiental entity) {
        return repository.save(entity);
    }
}
