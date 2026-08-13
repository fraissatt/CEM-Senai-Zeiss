package com.zeiss.pilot.controller;

import com.zeiss.pilot.entity.ItemAlmoxarifado;
import com.zeiss.pilot.entity.MovimentacaoAlmoxarifado;
import com.zeiss.pilot.service.AlmoxarifadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almoxarifado")
public class AlmoxarifadoController {

    private final AlmoxarifadoService service;

    public AlmoxarifadoController(AlmoxarifadoService service) {
        this.service = service;
    }

    @GetMapping("/itens")
    public ResponseEntity<List<ItemAlmoxarifado>> listarItens() {
        return ResponseEntity.ok(service.listarItens());
    }

    @PostMapping("/itens")
    public ResponseEntity<ItemAlmoxarifado> criarItem(@RequestBody ItemAlmoxarifado item) {
        return ResponseEntity.ok(service.salvarItem(item));
    }

    @PutMapping("/itens/{id}")
    public ResponseEntity<ItemAlmoxarifado> atualizarItem(@PathVariable Long id,
                                                            @RequestBody ItemAlmoxarifado item) {
        return ResponseEntity.ok(service.atualizarItem(id, item));
    }

    @DeleteMapping("/itens/{id}")
    public ResponseEntity<Void> deletarItem(@PathVariable Long id) {
        service.deletarItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movimentacoes")
    public ResponseEntity<List<MovimentacaoAlmoxarifado>> listarMovimentacoes(
            @RequestParam(required = false) Long itemId) {
        if (itemId != null) {
            return ResponseEntity.ok(service.listarMovimentacoesPorItem(itemId));
        }
        return ResponseEntity.ok(service.listarMovimentacoes());
    }

    @PostMapping("/movimentacoes")
    public ResponseEntity<MovimentacaoAlmoxarifado> registrarMovimentacao(
            @RequestBody MovimentacaoAlmoxarifado movimentacao) {
        return ResponseEntity.ok(service.registrarMovimentacao(movimentacao));
    }
}
