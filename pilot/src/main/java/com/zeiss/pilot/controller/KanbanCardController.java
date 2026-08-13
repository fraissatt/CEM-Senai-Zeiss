package com.zeiss.pilot.controller;

import com.zeiss.pilot.entity.KanbanCard;
import com.zeiss.pilot.service.KanbanCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kanban-cards")
public class KanbanCardController {

    private final KanbanCardService service;

    public KanbanCardController(KanbanCardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<KanbanCard>> listar(
            @RequestParam(required = false) Long estagiariaId) {
        if (estagiariaId != null) {
            return ResponseEntity.ok(service.listarPorEstagiaria(estagiariaId));
        }
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<KanbanCard> criar(@RequestBody KanbanCard card) {
        return ResponseEntity.ok(service.salvar(card));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<KanbanCard> atualizar(@PathVariable Long id, @RequestBody KanbanCard card) {
        return ResponseEntity.ok(service.atualizar(id, card));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
