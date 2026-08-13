package com.zeiss.pilot.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.zeiss.pilot.dto.EventoRelatorioDTO;
import com.zeiss.pilot.entity.Evento;
import com.zeiss.pilot.repository.EventoRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public List<Evento> getAllEventos() {
        return eventoRepository.findAll();
    }

    public Evento getEventoById(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com id: " + id));
    }

    public Evento createEvento(Evento evento) {
        if (evento.getNome() == null) evento.setNome("");
        evento.setTipo("Geral");
        if (evento.getDataEvento() == null) evento.setDataEvento(LocalDate.now());
        return eventoRepository.save(evento);
    }

    public Evento updateEvento(Long id, Evento dados) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com id: " + id));

        if (dados.getNome() != null) evento.setNome(dados.getNome());
        if (dados.getDataEvento() != null) evento.setDataEvento(dados.getDataEvento());
        evento.setDescricao(dados.getDescricao());
        evento.setHorario(dados.getHorario());
        evento.setLocal(dados.getLocal());
        evento.setResponsavel(dados.getResponsavel());
        evento.setNumeroParticipantes(dados.getNumeroParticipantes());
        evento.setObservacao(dados.getObservacao());

        return eventoRepository.save(evento);
    }

    public void deleteEvento(Long id) {
        eventoRepository.deleteById(id);
    }

    // 🔹 Método para calcular os relatórios de eventos
    public EventoRelatorioDTO getRelatoriosEventos() {
        List<Evento> eventos = eventoRepository.findAll();

        long totalEventos = eventos.size();
        double adesaoMedia = eventos.stream()
                .filter(e -> e.getNumeroConvidados() > 0)
                .mapToDouble(e -> (double) e.getNumeroPresentes() / e.getNumeroConvidados() * 100)
                .average()
                .orElse(0.0);

        // Distribuição mensal
        Map<String, Long> distribuicaoMensal = new HashMap<>();
        for (Month mes : Month.values()) {
            distribuicaoMensal.put(mes.toString(), 0L);
        }

        eventos.forEach(evento -> {
            if (evento.getDataEvento() == null) return;
            String mes = evento.getDataEvento().getMonth().toString();
            distribuicaoMensal.put(mes, distribuicaoMensal.getOrDefault(mes, 0L) + 1);
        });

        return new EventoRelatorioDTO(totalEventos, adesaoMedia, distribuicaoMensal);
    }
}
