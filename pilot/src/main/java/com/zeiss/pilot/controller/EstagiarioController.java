package com.zeiss.pilot.controller;

import com.zeiss.pilot.entity.Estagiario;
import com.zeiss.pilot.service.EstagiarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estagiarios")
public class EstagiarioController {

    private final EstagiarioService service;

    public EstagiarioController(EstagiarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Estagiario>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) {
            return ResponseEntity.ok(service.listarAtivos());
        }
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Estagiario> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Estagiario> criar(@RequestBody Estagiario estagiario) {
        return ResponseEntity.ok(service.salvar(estagiario));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Estagiario> atualizar(@PathVariable Long id, @RequestBody Estagiario estagiario) {
        return ResponseEntity.ok(service.atualizar(id, estagiario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
