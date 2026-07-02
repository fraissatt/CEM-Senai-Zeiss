package com.zeiss.pilot.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class KanbanCardTest {

    @Test
    void isVencido_semPrazo_falso() {
        KanbanCard card = new KanbanCard();
        card.setPrazo(null);
        card.setColuna("em-andamento");

        assertFalse(card.isVencido());
    }

    @Test
    void isVencido_prazoPassadoEColunaConcluido_falso() {
        KanbanCard card = new KanbanCard();
        card.setPrazo(LocalDate.now().minusDays(1));
        card.setColuna("concluido");

        assertFalse(card.isVencido());
    }

    @Test
    void isVencido_prazoPassadoEOutraColuna_verdadeiro() {
        KanbanCard card = new KanbanCard();
        card.setPrazo(LocalDate.now().minusDays(1));
        card.setColuna("em-andamento");

        assertTrue(card.isVencido());
    }

    @Test
    void isVencido_prazoFuturo_falso() {
        KanbanCard card = new KanbanCard();
        card.setPrazo(LocalDate.now().plusDays(1));
        card.setColuna("em-andamento");

        assertFalse(card.isVencido());
    }
}
