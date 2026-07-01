package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.Estagiario;
import com.zeiss.pilot.repository.EstagiarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstagiarioService {

    private final EstagiarioRepository repository;

    public EstagiarioService(EstagiarioRepository repository) {
        this.repository = repository;
    }

    public List<Estagiario> listar() {
        return repository.findAll();
    }

    public List<Estagiario> listarAtivos() {
        return repository.findByAtivoTrue();
    }

    public Estagiario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estagiário não encontrado: " + id));
    }

    public Estagiario salvar(Estagiario estagiario) {
        return repository.save(estagiario);
    }

    public Estagiario atualizar(Long id, Estagiario estagiario) {
        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estagiário não encontrado: " + id));
        estagiario.setId(id);
        return repository.save(estagiario);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
}
