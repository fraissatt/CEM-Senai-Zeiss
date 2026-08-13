package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.KanbanCard;
import com.zeiss.pilot.repository.KanbanCardRepository;
import com.zeiss.pilot.util.MapperUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KanbanCardService {

    private final KanbanCardRepository repository;

    public KanbanCardService(KanbanCardRepository repository) {
        this.repository = repository;
    }

    public List<KanbanCard> listar() {
        return repository.findAll();
    }

    public List<KanbanCard> listarPorEstagiaria(Long estagiariaId) {
        return repository.findByEstagiariaId(estagiariaId);
    }

    public KanbanCard salvar(KanbanCard card) {
        card.setId(null);
        return repository.save(card);
    }

    public KanbanCard atualizar(Long id, KanbanCard card) {
        KanbanCard existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card não encontrado: " + id));
        MapperUtil.copyNonNullProperties(card, existente);
        return repository.save(existente);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
}
