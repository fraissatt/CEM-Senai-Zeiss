package com.zeiss.pilot.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DocumentoMaquinaTest {

    @Test
    void recalcularStatus_semDataExpiracao_ficaAtivo() {
        DocumentoMaquina doc = new DocumentoMaquina();
        doc.setDataExpiracao(null);

        doc.recalcularStatus();

        assertEquals("ativo", doc.getStatus());
    }

    @Test
    void recalcularStatus_dataPassada_ficaExpirado() {
        DocumentoMaquina doc = new DocumentoMaquina();
        doc.setDataExpiracao(LocalDate.now().minusDays(1));

        doc.recalcularStatus();

        assertEquals("expirado", doc.getStatus());
    }

    @Test
    void recalcularStatus_dentroDeTrintaDias_ficaPrestesAVencer() {
        DocumentoMaquina doc = new DocumentoMaquina();
        doc.setDataExpiracao(LocalDate.now().plusDays(30));

        doc.recalcularStatus();

        assertEquals("prestes a vencer", doc.getStatus());
    }

    @Test
    void recalcularStatus_maisDeTrintaDias_ficaAtivo() {
        DocumentoMaquina doc = new DocumentoMaquina();
        doc.setDataExpiracao(LocalDate.now().plusDays(31));

        doc.recalcularStatus();

        assertEquals("ativo", doc.getStatus());
    }
}
