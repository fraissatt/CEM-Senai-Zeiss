package com.zeiss.pilot.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class PastaDocumentoTest {

    @Test
    void validarExclusao_naoLancaQuandoVazia() {
        PastaDocumento pasta = new PastaDocumento();
        assertDoesNotThrow(pasta::validarExclusao);
    }

    @Test
    void validarExclusao_lancaQuandoTemSubpastas() {
        PastaDocumento pasta = new PastaDocumento();
        pasta.setSubpastas(List.of(new PastaDocumento()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, pasta::validarExclusao);
        assertEquals("Exclusão bloqueada: existem subpastas.", ex.getMessage());
    }

    @Test
    void validarExclusao_lancaQuandoTemDocumentos() {
        PastaDocumento pasta = new PastaDocumento();
        pasta.setDocumentos(List.of(new DocumentoPDF()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, pasta::validarExclusao);
        assertEquals("Exclusão bloqueada: existem documentos vinculados.", ex.getMessage());
    }
}
