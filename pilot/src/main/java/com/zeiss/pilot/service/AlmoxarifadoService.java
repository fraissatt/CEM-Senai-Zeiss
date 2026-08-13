package com.zeiss.pilot.service;

import com.zeiss.pilot.entity.ItemAlmoxarifado;
import com.zeiss.pilot.entity.MovimentacaoAlmoxarifado;
import com.zeiss.pilot.repository.ItemAlmoxarifadoRepository;
import com.zeiss.pilot.repository.MovimentacaoAlmoxarifadoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlmoxarifadoService {

    private final ItemAlmoxarifadoRepository itemRepository;

    private final MovimentacaoAlmoxarifadoRepository movimentacaoRepository;

    public AlmoxarifadoService(ItemAlmoxarifadoRepository itemRepository, MovimentacaoAlmoxarifadoRepository movimentacaoRepository) {
        this.itemRepository = itemRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<ItemAlmoxarifado> listarItens() {
        return itemRepository.findAll();
    }

    public ItemAlmoxarifado salvarItem(ItemAlmoxarifado item) {
        item.setId(null);
        return itemRepository.save(item);
    }

    public ItemAlmoxarifado atualizarItem(Long id, ItemAlmoxarifado dto) {
        ItemAlmoxarifado existing = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item não encontrado: " + id));
        existing.setNome(dto.getNome());
        existing.setCategoria(dto.getCategoria());
        existing.setUnidade(dto.getUnidade());
        existing.setEstoqueMinimo(dto.getEstoqueMinimo());
        existing.setLocalizacao(dto.getLocalizacao());
        existing.setObservacao(dto.getObservacao());
        return itemRepository.save(existing);
    }

    public void deletarItem(Long id) {
        itemRepository.deleteById(id);
    }

    public List<MovimentacaoAlmoxarifado> listarMovimentacoes() {
        return movimentacaoRepository.findAll();
    }

    public List<MovimentacaoAlmoxarifado> listarMovimentacoesPorItem(Long itemId) {
        return movimentacaoRepository.findByItemId(itemId);
    }

    public MovimentacaoAlmoxarifado registrarMovimentacao(MovimentacaoAlmoxarifado dto) {
        Long itemId = dto.getItem() != null ? dto.getItem().getId() : null;
        if (itemId == null) {
            throw new RuntimeException("Item não informado para a movimentação");
        }
        ItemAlmoxarifado item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado: " + itemId));
        item.aplicarMovimentacao(dto.getTipo(), dto.getQuantidade());
        itemRepository.save(item);

        MovimentacaoAlmoxarifado m = new MovimentacaoAlmoxarifado();
        m.setItem(item);
        m.setTipo(dto.getTipo());
        m.setQuantidade(dto.getQuantidade());
        m.setResponsavel(dto.getResponsavel());
        m.setMotivo(dto.getMotivo());
        m.setData(dto.getData() != null ? dto.getData() : LocalDateTime.now());
        return movimentacaoRepository.save(m);
    }
}
