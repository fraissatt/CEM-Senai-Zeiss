package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.Amostra;
import com.zeiss.pilot.repository.AmostraRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class AmostraService {

    private final AmostraRepository repository;

    public AmostraService(AmostraRepository repository) {
        this.repository = repository;
    }

    public Page<Amostra> listarPaginado(int page, int size, String query, String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataEntrada"));
        String q = (query  != null && !query.trim().isEmpty())  ? query.trim()  : null;
        String s = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        return "Vencendo".equals(s)
                ? repository.searchVencendo(q, LocalDate.now(), pageable)
                : repository.search(q, s, pageable);
    }

    public Map<String, Long> getKpis() {
        LocalDate hoje = LocalDate.now();
        return Map.of(
                "total",      repository.count(),
                "custodia",   repository.countByStatus("Em custódia"),
                "vencendo",   repository.countByStatusAndDataDevPrevistaLessThanEqual("Em custódia", hoje),
                "devolvidas", repository.countByStatus("Devolvida")
        );
    }

    public Amostra buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amostra não encontrada: " + id));
    }

    public Amostra salvar(Amostra amostra) {
        normalizar(amostra);
        return repository.save(amostra);
    }

    public Amostra atualizar(Long id, Amostra amostra) {
        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amostra não encontrada: " + id));
        normalizar(amostra);
        amostra.setId(id);
        return repository.save(amostra);
    }

    // Preserva a normalização antes feita em AmostraDTO.toEntity(): status
    // default para "Em custódia" quando ausente.
    private void normalizar(Amostra amostra) {
        if (amostra.getStatus() == null) {
            amostra.setStatus("Em custódia");
        }
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
}
