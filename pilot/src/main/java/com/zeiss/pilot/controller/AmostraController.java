package com.zeiss.pilot.controller;

import com.zeiss.pilot.entity.Amostra;
import com.zeiss.pilot.service.AmostraService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/amostras")
public class AmostraController {

    private final AmostraService service;

    public AmostraController(AmostraService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Amostra>> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String query,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(service.listarPaginado(page, size, query, status));
    }

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Long>> kpis() {
        return ResponseEntity.ok(service.getKpis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Amostra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Amostra> criar(@RequestBody Amostra amostra) {
        return ResponseEntity.ok(service.salvar(amostra));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Amostra> atualizar(@PathVariable Long id, @RequestBody Amostra amostra) {
        return ResponseEntity.ok(service.atualizar(id, amostra));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
