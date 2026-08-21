package com.zeiss.pilot.service;

import com.zeiss.pilot.dto.RelatorioMensalDTO;
import com.zeiss.pilot.entity.Servico;
import com.zeiss.pilot.repository.ServicoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    public Page<Servico> listarPaginado(int page, int size, String query, String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        return servicoRepository.search(
                query  != null && !query.trim().isEmpty()  ? query.trim()  : null,
                status != null && !status.trim().isEmpty() ? status.trim() : null,
                pageable
        );
    }

    public Servico buscarPorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado: " + id));
    }

    public Servico criarServico(Servico servico) {
        normalizar(servico);
        return servicoRepository.save(servico);
    }

    public Servico atualizarServico(Long id, Servico servico) {
        servicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado: " + id));
        normalizar(servico);
        servico.setId(id);
        return servicoRepository.save(servico);
    }

    // Preserva a normalização antes feita em ServicoDTO.toEntity(): data de criação
    // default para hoje quando ausente e observação em branco vira null.
    private void normalizar(Servico servico) {
        if (servico.getDataCriacao() == null) {
            servico.setDataCriacao(LocalDate.now());
        }
        if (servico.getObservacao() != null && servico.getObservacao().trim().isEmpty()) {
            servico.setObservacao(null);
        }
        servico.setCpfOuCnpj(normalizarCpfOuCnpj(servico.getCpfOuCnpj()));

        if (servico.getValor() == null || servico.getValor().signum() <= 0) {
            throw new IllegalArgumentException("Valor inválido: informe um número maior que zero.");
        }
    }

    // Defesa em profundidade: reaplica a mesma máscara do frontend caso a API
    // seja chamada diretamente, sem passar pelo formulário.
    private String normalizarCpfOuCnpj(String cpfOuCnpj) {
        if (cpfOuCnpj == null || cpfOuCnpj.isBlank()) {
            return cpfOuCnpj;
        }
        String digits = cpfOuCnpj.replaceAll("\\D", "");
        if (digits.length() != 11 && digits.length() != 14) {
            throw new IllegalArgumentException(
                    "CPF/CNPJ inválido: informe 11 dígitos (CPF) ou 14 dígitos (CNPJ).");
        }
        return digits.length() == 11
                ? digits.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4")
                : digits.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    public List<RelatorioMensalDTO> obterRelatorioMensal() {
        return servicoRepository.calcularArrecadacaoMensal();
    }

    public void excluirServico(Long id) {
        servicoRepository.deleteById(id);
    }
}
