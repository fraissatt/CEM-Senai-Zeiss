package com.zeiss.pilot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zeiss.pilot.entity.NotaEstagiario;
import com.zeiss.pilot.repository.NotaEstagiarioRepository;

@Service
public class NotaEstagiarioService {

    private final NotaEstagiarioRepository repository;

    public NotaEstagiarioService(NotaEstagiarioRepository repository) {
        this.repository = repository;
    }

    public List<NotaEstagiario> listar() {
        return repository.findAll();
    }

    public List<NotaEstagiario> listarPorEstagiaria(Long estagiariaId) {
        return repository.findByEstagiariaId(estagiariaId);
    }

    public NotaEstagiario salvar(NotaEstagiario nota) {
        return repository.save(nota);
    }
}
