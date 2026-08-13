package com.zeiss.pilot.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ItemAlmoxarifadoTest {

    @Test
    void aplicarMovimentacao_entrada_somaQuantidade() {
        ItemAlmoxarifado item = new ItemAlmoxarifado();
        item.setQuantidadeAtual(10);

        item.aplicarMovimentacao("entrada", 5);

        assertEquals(15, item.getQuantidadeAtual());
    }

    @Test
    void aplicarMovimentacao_entradaCaseInsensitive_somaQuantidade() {
        ItemAlmoxarifado item = new ItemAlmoxarifado();
        item.setQuantidadeAtual(10);

        item.aplicarMovimentacao("ENTRADA", 5);

        assertEquals(15, item.getQuantidadeAtual());
    }

    @Test
    void aplicarMovimentacao_saida_subtraiQuantidade() {
        ItemAlmoxarifado item = new ItemAlmoxarifado();
        item.setQuantidadeAtual(10);

        item.aplicarMovimentacao("saida", 4);

        assertEquals(6, item.getQuantidadeAtual());
    }

    @Test
    void aplicarMovimentacao_saidaMaiorQueEstoque_ficaEmZero() {
        ItemAlmoxarifado item = new ItemAlmoxarifado();
        item.setQuantidadeAtual(3);

        item.aplicarMovimentacao("saida", 10);

        assertEquals(0, item.getQuantidadeAtual());
    }
}
