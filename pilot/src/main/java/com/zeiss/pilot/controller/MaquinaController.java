package com.zeiss.pilot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.entity.AgendamentoMaquina;
import com.zeiss.pilot.entity.ManutencaoMaquina;
import com.zeiss.pilot.entity.Maquina;
import com.zeiss.pilot.entity.SessaoMaquina;
import com.zeiss.pilot.service.MaquinaService;

@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

    private final MaquinaService service;

    public MaquinaController(MaquinaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Maquina>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Maquina> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Maquina> criar(@RequestBody Maquina maquina) {
        return ResponseEntity.ok(service.salvar(maquina));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Maquina> atualizar(@PathVariable Long id, @RequestBody Maquina maquina) {
        return ResponseEntity.ok(service.atualizar(id, maquina));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // Sessões

    @GetMapping("/{maquinaId}/sessoes")
    public ResponseEntity<List<SessaoMaquina>> listarSessoes(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(service.listarSessoes(maquinaId));
    }

    @PostMapping("/{maquinaId}/sessoes")
    public ResponseEntity<SessaoMaquina> criarSessao(@PathVariable Long maquinaId,
                                                       @RequestBody SessaoMaquina sessao) {
        return ResponseEntity.ok(service.criarSessao(maquinaId, sessao));
    }

    @PatchMapping("/{maquinaId}/sessoes/{id}")
    public ResponseEntity<SessaoMaquina> atualizarSessao(@PathVariable Long maquinaId,
                                                           @PathVariable Long id,
                                                           @RequestBody SessaoMaquina sessao) {
        return ResponseEntity.ok(service.atualizarSessao(id, sessao));
    }

    @DeleteMapping("/{maquinaId}/sessoes/{id}")
    public ResponseEntity<Void> deletarSessao(@PathVariable Long maquinaId, @PathVariable Long id) {
        service.deletarSessao(id);
        return ResponseEntity.noContent().build();
    }

    // Manutenções

    @GetMapping("/{maquinaId}/manutencoes")
    public ResponseEntity<List<ManutencaoMaquina>> listarManutencoes(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(service.listarManutencoes(maquinaId));
    }

    @PostMapping("/{maquinaId}/manutencoes")
    public ResponseEntity<ManutencaoMaquina> criarManutencao(@PathVariable Long maquinaId,
                                                                @RequestBody ManutencaoMaquina manutencao) {
        return ResponseEntity.ok(service.criarManutencao(maquinaId, manutencao));
    }

    @PatchMapping("/{maquinaId}/manutencoes/{id}")
    public ResponseEntity<ManutencaoMaquina> atualizarManutencao(@PathVariable Long maquinaId,
                                                                    @PathVariable Long id,
                                                                    @RequestBody ManutencaoMaquina manutencao) {
        return ResponseEntity.ok(service.atualizarManutencao(id, manutencao));
    }

    @DeleteMapping("/{maquinaId}/manutencoes/{id}")
    public ResponseEntity<Void> deletarManutencao(@PathVariable Long maquinaId, @PathVariable Long id) {
        service.deletarManutencao(id);
        return ResponseEntity.noContent().build();
    }

    // Agendamentos

    @GetMapping("/{maquinaId}/agendamentos")
    public ResponseEntity<List<AgendamentoMaquina>> listarAgendamentos(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(service.listarAgendamentos(maquinaId));
    }

    @PostMapping("/{maquinaId}/agendamentos")
    public ResponseEntity<AgendamentoMaquina> criarAgendamento(@PathVariable Long maquinaId,
                                                                  @RequestBody AgendamentoMaquina agendamento) {
        return ResponseEntity.ok(service.criarAgendamento(maquinaId, agendamento));
    }

    @PatchMapping("/{maquinaId}/agendamentos/{id}")
    public ResponseEntity<AgendamentoMaquina> atualizarAgendamento(@PathVariable Long maquinaId,
                                                                      @PathVariable Long id,
                                                                      @RequestBody AgendamentoMaquina agendamento) {
        return ResponseEntity.ok(service.atualizarAgendamento(id, agendamento));
    }

    @DeleteMapping("/{maquinaId}/agendamentos/{id}")
    public ResponseEntity<Void> deletarAgendamento(@PathVariable Long maquinaId, @PathVariable Long id) {
        service.deletarAgendamento(id);
        return ResponseEntity.noContent().build();
    }

}
