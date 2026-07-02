# Phase 2 Round 1 — Domain Behavior in Entities — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move 5 pieces of single-entity business logic (validation, state derivation, stock adjustment) out of Service classes and into their Entities as explicit domain methods, following the pattern already established by `Maquina.ligar()`/`desligar()` and `Servico.finalizar()`.

**Architecture:** Spring Boot monolith (Java 21, Spring Boot 3.4.4). Each task adds one or two `public void`/`public boolean` methods to a JPA `@Entity` class (pure logic over the entity's own fields, no repository/bean access), then updates the corresponding `@Service` class to call the new method instead of containing the logic inline. No Controller, DTO, route, or HTTP status changes in this plan.

**Tech Stack:** Java 21, Spring Boot 3.4.4, JUnit 5 (via `spring-boot-starter-test`), Maven.

## Global Constraints

- No changes to any Controller class, REST route, HTTP verb, or HTTP status code mapping.
- No changes to JSON field names/shapes returned to the frontend.
- `DocumentoPDF` and `DocumentoPDFService` must not be touched (Task 13g of Phase 1 is deliberately deferred — active unrelated development in that area).
- Entity domain methods must not take a `Repository` or Spring-managed bean as a parameter or field — they operate only on `this`'s own fields (and, where noted, on already-loaded in-memory collections).
- Exception types/messages thrown by moved logic must stay byte-for-byte identical to what the Service throws today, so existing Controller `catch` blocks keep working unchanged.
- Every new Entity method gets a plain JUnit 5 test class in `pilot/src/test/java/com/zeiss/pilot/entity/`, with no Spring context (`@SpringBootTest`) and no mocks — these are pure-function tests over `new Entity()` instances.

---

## Task 1: `PastaDocumento.validarExclusao()`

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/entity/PastaDocumento.java`
- Modify: `pilot/src/main/java/com/zeiss/pilot/service/PastaDocumentoService.java:62-71`
- Test: `pilot/src/test/java/com/zeiss/pilot/entity/PastaDocumentoTest.java`

**Interfaces:**
- Produces: `public void PastaDocumento.validarExclusao()` — throws `IllegalStateException("Exclusão bloqueada: existem subpastas.")` if `getSubpastas()` is non-empty; throws `IllegalStateException("Exclusão bloqueada: existem documentos vinculados.")` if `getDocumentos()` is non-empty; otherwise returns normally.

- [ ] **Step 1: Write the failing test**

Create `pilot/src/test/java/com/zeiss/pilot/entity/PastaDocumentoTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=PastaDocumentoTest -q`
Expected: FAIL (compile error) — `cannot find symbol: method validarExclusao()`

- [ ] **Step 3: Implement `validarExclusao()` on the entity**

In `pilot/src/main/java/com/zeiss/pilot/entity/PastaDocumento.java`, add after the `getSubpastas()`/`setSubpastas()` getters/setters (after line 65, before the closing `}` of the class):

```java

    // Impede a exclusão de uma pasta que ainda tenha subpastas ou documentos vinculados.
    public void validarExclusao() {
        if (!subpastas.isEmpty()) {
            throw new IllegalStateException("Exclusão bloqueada: existem subpastas.");
        }
        if (!documentos.isEmpty()) {
            throw new IllegalStateException("Exclusão bloqueada: existem documentos vinculados.");
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=PastaDocumentoTest -q`
Expected: PASS (3 tests, 0 failures)

- [ ] **Step 5: Update the Service to call the new method**

In `pilot/src/main/java/com/zeiss/pilot/service/PastaDocumentoService.java`, replace lines 62-71:

```java
    public void excluir(Long id) {
        PastaDocumento pasta = obter(id);
        if (!pasta.getSubpastas().isEmpty()) {
            throw new IllegalStateException("Exclusão bloqueada: existem subpastas.");
        }
        if (!pasta.getDocumentos().isEmpty()) {
            throw new IllegalStateException("Exclusão bloqueada: existem documentos vinculados.");
        }
        repo.delete(pasta);
    }
```

with:

```java
    public void excluir(Long id) {
        PastaDocumento pasta = obter(id);
        pasta.validarExclusao();
        repo.delete(pasta);
    }
```

- [ ] **Step 6: Compile the whole project**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/entity/PastaDocumento.java pilot/src/main/java/com/zeiss/pilot/service/PastaDocumentoService.java pilot/src/test/java/com/zeiss/pilot/entity/PastaDocumentoTest.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor(pasta-documento): move exclusion precondition into PastaDocumento.validarExclusao()"
```

---

## Task 2: `DocumentoMaquina.recalcularStatus()`

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/entity/DocumentoMaquina.java`
- Modify: `pilot/src/main/java/com/zeiss/pilot/service/DocumentoMaquinaService.java:45-108`
- Test: `pilot/src/test/java/com/zeiss/pilot/entity/DocumentoMaquinaTest.java`

**Interfaces:**
- Produces: `public void DocumentoMaquina.recalcularStatus()` — reads `this.dataExpiracao`, sets `this.status` to `"ativo"` (null date), `"expirado"` (date before today), `"prestes a vencer"` (date within the next 30 days inclusive), or `"ativo"` (otherwise).

- [ ] **Step 1: Write the failing test**

Create `pilot/src/test/java/com/zeiss/pilot/entity/DocumentoMaquinaTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=DocumentoMaquinaTest -q`
Expected: FAIL (compile error) — `cannot find symbol: method recalcularStatus()`

- [ ] **Step 3: Implement `recalcularStatus()` on the entity**

In `pilot/src/main/java/com/zeiss/pilot/entity/DocumentoMaquina.java`, add the import at the top (after `import java.time.LocalDate;` on line 5, no new import needed since `LocalDate` is already imported) and add the method after `setStatus` (after line 63, before the closing `}` of the class):

```java

    // Deriva o status a partir da data de expiração: sem data = ativo; já expirada = expirado;
    // vencendo nos próximos 30 dias = prestes a vencer; caso contrário, ativo.
    public void recalcularStatus() {
        if (dataExpiracao == null) {
            this.status = "ativo";
            return;
        }
        LocalDate hoje = LocalDate.now();
        if (dataExpiracao.isBefore(hoje)) {
            this.status = "expirado";
        } else if (!dataExpiracao.isAfter(hoje.plusDays(30))) {
            this.status = "prestes a vencer";
        } else {
            this.status = "ativo";
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=DocumentoMaquinaTest -q`
Expected: PASS (4 tests, 0 failures)

- [ ] **Step 5: Update the Service to call the new method**

In `pilot/src/main/java/com/zeiss/pilot/service/DocumentoMaquinaService.java`:

Replace line 67 (`doc.setStatus(calcularStatus(dataExpiracao));`) with:

```java
        doc.recalcularStatus();
```

Replace line 75 (`.peek(doc -> doc.setStatus(calcularStatus(doc.getDataExpiracao())))`) with:

```java
                .peek(DocumentoMaquina::recalcularStatus)
```

Delete the now-unused private method (lines 102-108):

```java
    private String calcularStatus(LocalDate dataExpiracao) {
        if (dataExpiracao == null) return "ativo";
        LocalDate hoje = LocalDate.now();
        if (dataExpiracao.isBefore(hoje)) return "expirado";
        if (!dataExpiracao.isAfter(hoje.plusDays(30))) return "prestes a vencer";
        return "ativo";
    }
```

- [ ] **Step 6: Compile the whole project**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd compile -q`
Expected: BUILD SUCCESS. If `java.time.LocalDate` import in `DocumentoMaquinaService.java` becomes unused, leave it — Maven does not fail on unused imports.

- [ ] **Step 7: Commit**

```bash
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/entity/DocumentoMaquina.java pilot/src/main/java/com/zeiss/pilot/service/DocumentoMaquinaService.java pilot/src/test/java/com/zeiss/pilot/entity/DocumentoMaquinaTest.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor(documento-maquina): move status derivation into DocumentoMaquina.recalcularStatus()"
```

---

## Task 3: `Usuario.derivarRoleDoCargo()` + `Usuario.validarSenhaObrigatoria()`

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/entity/Usuario.java`
- Modify: `pilot/src/main/java/com/zeiss/pilot/service/UsuarioService.java:24-63`
- Test: `pilot/src/test/java/com/zeiss/pilot/entity/UsuarioTest.java`

**Interfaces:**
- Produces: `public void Usuario.derivarRoleDoCargo()` — sets `this.role` to `"ADMIN"` if `this.cargo` is non-null and not equal (case-insensitive) to `"ESTAGIARIO"`, otherwise sets it to `"CLIENTE"`.
- Produces: `public void Usuario.validarSenhaObrigatoria()` — throws `IllegalArgumentException("Senha obrigatória para criar usuário")` if `this.senha` is null or blank; otherwise returns normally.

- [ ] **Step 1: Write the failing test**

Create `pilot/src/test/java/com/zeiss/pilot/entity/UsuarioTest.java`:

```java
package com.zeiss.pilot.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void derivarRoleDoCargo_semCargo_viraCliente() {
        Usuario usuario = new Usuario();
        usuario.setCargo(null);

        usuario.derivarRoleDoCargo();

        assertEquals("CLIENTE", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_estagiario_viraCliente() {
        Usuario usuario = new Usuario();
        usuario.setCargo("estagiario");

        usuario.derivarRoleDoCargo();

        assertEquals("CLIENTE", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_outroCargo_viraAdmin() {
        Usuario usuario = new Usuario();
        usuario.setCargo("Gerente");

        usuario.derivarRoleDoCargo();

        assertEquals("ADMIN", usuario.getRole());
    }

    @Test
    void validarSenhaObrigatoria_senhaValida_naoLanca() {
        Usuario usuario = new Usuario();
        usuario.setSenha("segredo123");

        assertDoesNotThrow(usuario::validarSenhaObrigatoria);
    }

    @Test
    void validarSenhaObrigatoria_senhaNula_lanca() {
        Usuario usuario = new Usuario();
        usuario.setSenha(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, usuario::validarSenhaObrigatoria);
        assertEquals("Senha obrigatória para criar usuário", ex.getMessage());
    }

    @Test
    void validarSenhaObrigatoria_senhaEmBranco_lanca() {
        Usuario usuario = new Usuario();
        usuario.setSenha("   ");

        assertThrows(IllegalArgumentException.class, usuario::validarSenhaObrigatoria);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=UsuarioTest -q`
Expected: FAIL (compile error) — `cannot find symbol: method derivarRoleDoCargo()` / `validarSenhaObrigatoria()`

- [ ] **Step 3: Implement both methods on the entity**

In `pilot/src/main/java/com/zeiss/pilot/entity/Usuario.java`, add after `setDataCriacao` (after line 63, before the closing `}` of the class):

```java

    // Deriva o role de segurança a partir do cargo organizacional: ESTAGIARIO (ou sem cargo) = CLIENTE, qualquer outro = ADMIN.
    public void derivarRoleDoCargo() {
        if (cargo != null && !cargo.equalsIgnoreCase("ESTAGIARIO")) {
            this.role = "ADMIN";
        } else {
            this.role = "CLIENTE";
        }
    }

    public void validarSenhaObrigatoria() {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória para criar usuário");
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=UsuarioTest -q`
Expected: PASS (6 tests, 0 failures)

- [ ] **Step 5: Update the Service to call the new methods**

In `pilot/src/main/java/com/zeiss/pilot/service/UsuarioService.java`, replace the body of `criarUsuario` (lines 24-38):

```java
    public Usuario criarUsuario(Usuario usuario) {
        // Derive security role from organizational cargo
        if (usuario.getCargo() != null && !usuario.getCargo().equalsIgnoreCase("ESTAGIARIO")) {
            usuario.setRole("ADMIN");
        } else {
            usuario.setRole("CLIENTE");
        }

        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória para criar usuário");
        }
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        return usuarioRepository.save(usuario);
    }
```

with:

```java
    public Usuario criarUsuario(Usuario usuario) {
        usuario.derivarRoleDoCargo();
        usuario.validarSenhaObrigatoria();
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        return usuarioRepository.save(usuario);
    }
```

Then replace the cargo/role block inside `atualizarUsuario` (lines 52-56):

```java
        if (usuarioAtualizado.getCargo() != null) {
            existente.setCargo(usuarioAtualizado.getCargo());
            String derivedRole = "ESTAGIARIO".equalsIgnoreCase(usuarioAtualizado.getCargo()) ? "CLIENTE" : "ADMIN";
            existente.setRole(derivedRole);
        }
```

with:

```java
        if (usuarioAtualizado.getCargo() != null) {
            existente.setCargo(usuarioAtualizado.getCargo());
            existente.derivarRoleDoCargo();
        }
```

- [ ] **Step 6: Compile the whole project**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/entity/Usuario.java pilot/src/main/java/com/zeiss/pilot/service/UsuarioService.java pilot/src/test/java/com/zeiss/pilot/entity/UsuarioTest.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor(usuario): move role derivation and password validation into Usuario entity"
```

---

## Task 4: `ItemAlmoxarifado.aplicarMovimentacao(String, int)`

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/entity/ItemAlmoxarifado.java`
- Modify: `pilot/src/main/java/com/zeiss/pilot/service/AlmoxarifadoService.java:57-76`
- Test: `pilot/src/test/java/com/zeiss/pilot/entity/ItemAlmoxarifadoTest.java`

**Interfaces:**
- Produces: `public void ItemAlmoxarifado.aplicarMovimentacao(String tipo, int quantidade)` — if `tipo.equalsIgnoreCase("entrada")`, adds `quantidade` to `this.quantidadeAtual`; otherwise subtracts it; result is floored at `0` (`Math.max(0, ...)`).

- [ ] **Step 1: Write the failing test**

Create `pilot/src/test/java/com/zeiss/pilot/entity/ItemAlmoxarifadoTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=ItemAlmoxarifadoTest -q`
Expected: FAIL (compile error) — `cannot find symbol: method aplicarMovimentacao(java.lang.String,int)`

- [ ] **Step 3: Implement `aplicarMovimentacao` on the entity**

In `pilot/src/main/java/com/zeiss/pilot/entity/ItemAlmoxarifado.java`, add after `setObservacao` (after line 42, before the closing `}` of the class):

```java

    // Ajusta o estoque conforme o tipo de movimentação ("entrada" soma, qualquer outro valor subtrai), sem permitir valor negativo.
    public void aplicarMovimentacao(String tipo, int quantidade) {
        int delta = "entrada".equalsIgnoreCase(tipo) ? quantidade : -quantidade;
        this.quantidadeAtual = Math.max(0, this.quantidadeAtual + delta);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=ItemAlmoxarifadoTest -q`
Expected: PASS (4 tests, 0 failures)

- [ ] **Step 5: Update the Service to call the new method**

In `pilot/src/main/java/com/zeiss/pilot/service/AlmoxarifadoService.java`, replace lines 64-66:

```java
        int delta = "entrada".equalsIgnoreCase(dto.getTipo()) ? dto.getQuantidade() : -dto.getQuantidade();
        item.setQuantidadeAtual(Math.max(0, item.getQuantidadeAtual() + delta));
        itemRepository.save(item);
```

with:

```java
        item.aplicarMovimentacao(dto.getTipo(), dto.getQuantidade());
        itemRepository.save(item);
```

- [ ] **Step 6: Compile the whole project**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/entity/ItemAlmoxarifado.java pilot/src/main/java/com/zeiss/pilot/service/AlmoxarifadoService.java pilot/src/test/java/com/zeiss/pilot/entity/ItemAlmoxarifadoTest.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor(almoxarifado): move stock adjustment into ItemAlmoxarifado.aplicarMovimentacao()"
```

---

## Task 5: `KanbanCard.isVencido()`

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/entity/KanbanCard.java`
- Modify: `pilot/src/main/java/com/zeiss/pilot/service/DashboardEstagiarioService.java:51-57`
- Test: `pilot/src/test/java/com/zeiss/pilot/entity/KanbanCardTest.java`

**Interfaces:**
- Produces: `public boolean KanbanCard.isVencido()` — `true` if `this.prazo` is non-null, before today, and `this.coluna` is not (case-insensitive) `"concluido"`; `false` otherwise.

- [ ] **Step 1: Write the failing test**

Create `pilot/src/test/java/com/zeiss/pilot/entity/KanbanCardTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=KanbanCardTest -q`
Expected: FAIL (compile error) — `cannot find symbol: method isVencido()`

- [ ] **Step 3: Implement `isVencido()` on the entity**

In `pilot/src/main/java/com/zeiss/pilot/entity/KanbanCard.java`, add the `java.time.LocalDate` import (already imported on line 4) and add the method after `setCriadoEm` (after line 56, before the closing `}` of the class):

```java

    // Um card está vencido se tiver prazo definido, esse prazo já passou, e a coluna não é "concluido".
    public boolean isVencido() {
        return prazo != null && prazo.isBefore(LocalDate.now()) && !"concluido".equalsIgnoreCase(coluna);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -Dtest=KanbanCardTest -q`
Expected: PASS (4 tests, 0 failures)

- [ ] **Step 5: Update the Service to call the new method**

In `pilot/src/main/java/com/zeiss/pilot/service/DashboardEstagiarioService.java`, replace lines 51-57:

```java
            for (KanbanCard c : cards) {
                porColuna.merge(c.getColuna(), 1, Integer::sum);
                if (c.getPrazo() != null && c.getPrazo().isBefore(LocalDate.now())
                        && !"concluido".equalsIgnoreCase(c.getColuna())) {
                    vencidas++;
                }
            }
```

with:

```java
            for (KanbanCard c : cards) {
                porColuna.merge(c.getColuna(), 1, Integer::sum);
                if (c.isVencido()) {
                    vencidas++;
                }
            }
```

- [ ] **Step 6: Compile the whole project**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd compile -q`
Expected: BUILD SUCCESS. If `java.time.LocalDate` import in `DashboardEstagiarioService.java` becomes unused, leave it — Maven does not fail on unused imports.

- [ ] **Step 7: Commit**

```bash
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/entity/KanbanCard.java pilot/src/main/java/com/zeiss/pilot/service/DashboardEstagiarioService.java pilot/src/test/java/com/zeiss/pilot/entity/KanbanCardTest.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor(kanban): move overdue-card check into KanbanCard.isVencido()"
```

---

## Task 6: Full build, full test suite, and manual regression pass

**Files:** None (verification only).

- [ ] **Step 1: Run the full test suite**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd test -q`
Expected: BUILD SUCCESS, all tests pass (including the 21 new entity tests from Tasks 1-5 and the pre-existing `PilotApplicationTests`).

- [ ] **Step 2: Full package build**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd package -DskipTests -q`
Expected: BUILD SUCCESS, JAR generated in `target/`.

- [ ] **Step 3: Start the server**

Run: `cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot" && .\mvnw.cmd spring-boot:run`
Expected: server starts on `http://localhost:8090` with no startup errors.

- [ ] **Step 4: Manual regression checklist (via browser/curl, authenticated session)**

Verify each touched endpoint still behaves as before (same response shape, same status codes):

| Ação | Endpoint | Esperado |
|---|---|---|
| Excluir pasta vazia | `DELETE /api/pastas/{id}` (pasta sem subpastas/documentos) | `204 No Content` |
| Excluir pasta com subpasta | `DELETE /api/pastas/{id}` (pasta com subpasta) | `409 Conflict`, body `"Exclusão bloqueada: existem subpastas."` |
| Upload documento de máquina com data futura distante | `POST /api/maquinas/{id}/documentos` | Documento criado com `status: "ativo"` |
| Upload documento de máquina com data em 10 dias | `POST /api/maquinas/{id}/documentos` | Documento criado com `status: "prestes a vencer"` |
| Criar usuário com cargo "Gerente" | `POST /api/usuarios` | Usuário criado com `role: "ADMIN"` |
| Criar usuário com cargo "Estagiario" | `POST /api/usuarios` | Usuário criado com `role: "CLIENTE"` |
| Criar usuário sem senha | `POST /api/usuarios` | `400 Bad Request` |
| Registrar movimentação "entrada" | `POST /api/almoxarifado/movimentacoes` | `quantidadeAtual` do item aumenta na quantidade informada |
| Registrar movimentação "saida" maior que o estoque | `POST /api/almoxarifado/movimentacoes` | `quantidadeAtual` do item fica em `0` (não fica negativo) |
| Ver dashboard de estagiários com card vencido | `GET /api/dashboard-estagiarios` (ou rota equivalente) | Card com prazo passado e coluna diferente de "concluido" conta em `vencidas` |

- [ ] **Step 5: Stop the server, commit final verification marker**

```bash
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit --allow-empty -m "chore: phase 2 round 1 domain behavior verified end-to-end"
```
