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

import com.zeiss.pilot.entity.Edital;
import com.zeiss.pilot.service.EditalService;

@RestController
@RequestMapping("/api/editais")
public class EditalController {

    private final EditalService editalService;

    public EditalController(EditalService editalService) {
        this.editalService = editalService;
    }

    @GetMapping
    public ResponseEntity<List<Edital>> listarTodos() {
        return ResponseEntity.ok(editalService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Edital> buscarPorId(@PathVariable Long id) {
        Edital edital = editalService.buscarPorId(id);
        return edital != null ? ResponseEntity.ok(edital) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Edital> criarEdital(@RequestBody Edital edital) {
        try {
            return ResponseEntity.ok(editalService.criarEdital(edital));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Edital> atualizarEdital(@PathVariable Long id, @RequestBody Edital edital) {
        try {
            edital.setId(id);
            return ResponseEntity.ok(editalService.atualizarEdital(id, edital));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirEdital(@PathVariable Long id) {
        try {
            editalService.excluirEdital(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
