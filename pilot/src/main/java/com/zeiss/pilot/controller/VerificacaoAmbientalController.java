package com.zeiss.pilot.controller;

import com.zeiss.pilot.entity.VerificacaoAmbiental;
import com.zeiss.pilot.service.VerificacaoAmbientalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/verificacoes-ambientais")
public class VerificacaoAmbientalController {

    private final VerificacaoAmbientalService service;

    public VerificacaoAmbientalController(VerificacaoAmbientalService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<VerificacaoAmbiental>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<VerificacaoAmbiental> criar(@RequestBody VerificacaoAmbiental entity) {
        return ResponseEntity.ok(service.salvar(entity));
    }
}
