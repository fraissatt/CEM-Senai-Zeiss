package com.zeiss.pilot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.entity.VisitaTecnica;
import com.zeiss.pilot.service.VisitaTecnicaService;

@RestController
@RequestMapping("/api/visitas-tecnicas")
public class VisitaTecnicaController {

    private final VisitaTecnicaService service;

    public VisitaTecnicaController(VisitaTecnicaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<VisitaTecnica>> getAllVisitas() {
        return ResponseEntity.ok(service.listarVisitas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitaTecnica> buscarVisita(@PathVariable Long id) {
        return service.buscarPorId(id)
                       .map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VisitaTecnica> salvarVisita(@RequestBody VisitaTecnica visita) {
        return ResponseEntity.ok(service.salvarVisita(visita));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitaTecnica> atualizarVisita(@PathVariable Long id, @RequestBody VisitaTecnica visita) {
        visita.setId(id);
        return ResponseEntity.ok(service.salvarVisita(visita));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarVisita(@PathVariable Long id) {
        service.deletarVisita(id);
        return ResponseEntity.noContent().build();
    }
}
