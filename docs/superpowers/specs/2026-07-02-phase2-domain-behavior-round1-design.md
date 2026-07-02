# Phase 2 — Comportamento de Domínio nas Entities (Rodada 1)

**Status:** Design aprovado, aguardando plano de implementação.
**Escopo:** Backend apenas — nenhuma mudança de contrato de API (mesmas rotas, verbos, status HTTP de erro e formato de JSON). Frontend não é tocado nesta rodada.
**Branch:** a definir (sugestão: `refactor/phase2-domain-behavior-round1`, a partir de `main` pós-merge da Phase 1).
**Depende de:** Phase 1 concluída ([`docs/REFACTOR_PHASE1.md`](../../REFACTOR_PHASE1.md)) — Waves 1-4, 19/20 entities unificadas.
**Decisão de ordem da Fase 2:** ver memória de projeto `project_refactor_phase2_order` — domínio nas Entities e Services orquestradores vêm antes de Case-Use DTOs por endpoint.

## Objetivo

Mover para dentro das Entities a lógica de negócio que hoje vive nos Services e que opera **apenas sobre o próprio estado da Entity** (validação, transição de estado, valor derivado) — sem envolver outros repositories/services. Os Services passam a chamar esses métodos em vez de conter a regra, reduzindo duplicação e aproximando o modelo de um domain model rico. Nenhuma mudança de contrato de API: mesmos endpoints, mesmos status HTTP de erro.

Este documento cobre a **primeira rodada**: os 5 candidatos que seguem o mesmo padrão já validado na Phase 1 (`Maquina.ligar()`/`desligar()`, `Servico.finalizar()`) — método imperativo, mutação do próprio estado, exceção lançada pela Entity quando a regra é violada.

## Levantamento (como chegamos nos 5 candidatos)

Um agente de exploração revisou as 18 classes de Service do projeto (`pilot/src/main/java/com/zeiss/pilot/service/`) procurando lógica de validação/transição de estado/valor derivado operando sobre uma única Entity. O levantamento completo identificou duas categorias:

1. **Regras de negócio reais** (mesmo padrão de `ligar()`/`finalizar()`) — os 5 candidatos desta rodada.
2. **Cópia de campos** (`atualizarDados()`/`normalizar()`) — mecânico, menor risco, mas menos "regra de domínio". Fica para uma rodada seguinte: `Maquina`/`SessaoMaquina`/`ManutencaoMaquina`/`AgendamentoMaquina`, `Edital`, `Projeto` (parcial), `Evento` (inclui reconciliar o campo `adesao` hoje não utilizado no cálculo de taxa de adesão), `Servico.normalizar()`, `Amostra.normalizar()`.

`DocumentoPDF.recalcularStatus()` foi identificado como candidato do mesmo tipo que `DocumentoMaquina.recalcularStatus()`, mas **fica fora desta rodada**: a unificação de `DocumentoPDF` (Task 13g da Phase 1) foi deliberadamente adiada porque o usuário estava desenvolvendo ativamente nessa área (`DocumentoPDFDTO.java` ainda existe). Revisitar quando a Task 13g for retomada.

## Escopo — 5 mudanças

### 1. `PastaDocumento.validarExclusao()`

**Hoje** (`PastaDocumentoService.excluir()`, linhas 62-70):
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

**Depois:**
- `PastaDocumento.validarExclusao()` (void) — mesmas duas checagens, mesmas mensagens, lançando as mesmas `IllegalStateException`.
- `PastaDocumentoService.excluir()` fica: `PastaDocumento pasta = obter(id); pasta.validarExclusao(); repo.delete(pasta);`.
- `PastaDocumentoController` não muda — o `catch (IllegalStateException e) → 409 CONFLICT` já existente continua funcionando.

### 2. `DocumentoMaquina.recalcularStatus()`

**Hoje** (`DocumentoMaquinaService`, método privado `calcularStatus(LocalDate)`, linhas 102-108, chamado em `upload()` linha 67 e `listar()` linha 75):
```java
private String calcularStatus(LocalDate dataExpiracao) {
    if (dataExpiracao == null) return "ativo";
    LocalDate hoje = LocalDate.now();
    if (dataExpiracao.isBefore(hoje)) return "expirado";
    if (!dataExpiracao.isAfter(hoje.plusDays(30))) return "prestes a vencer";
    return "ativo";
}
```

**Depois:**
- `DocumentoMaquina.recalcularStatus()` (void) — mesma regra, mutando `this.status` a partir de `this.dataExpiracao`.
- `upload()`: `doc.setDataExpiracao(dataExpiracao); doc.recalcularStatus();` no lugar de `doc.setStatus(calcularStatus(dataExpiracao))`.
- `listar()`: `.peek(DocumentoMaquina::recalcularStatus)` no lugar de `.peek(doc -> doc.setStatus(calcularStatus(doc.getDataExpiracao())))`.
- Remover o método privado `calcularStatus` do Service.

### 3. `Usuario.derivarRoleDoCargo()` + `Usuario.validarSenhaObrigatoria()`

**Hoje** (`UsuarioService`, regra duplicada em `criarUsuario()` linhas 26-30 e `atualizarUsuario()` linhas 52-56; validação de senha em `criarUsuario()` linhas 32-34):
```java
// criarUsuario
if (usuario.getCargo() != null && !usuario.getCargo().equalsIgnoreCase("ESTAGIARIO")) {
    usuario.setRole("ADMIN");
} else {
    usuario.setRole("CLIENTE");
}
if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
    throw new IllegalArgumentException("Senha obrigatória para criar usuário");
}
usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

// atualizarUsuario
if (usuarioAtualizado.getCargo() != null) {
    existente.setCargo(usuarioAtualizado.getCargo());
    String derivedRole = "ESTAGIARIO".equalsIgnoreCase(usuarioAtualizado.getCargo()) ? "CLIENTE" : "ADMIN";
    existente.setRole(derivedRole);
}
```

**Decisão de design (confirmada com o usuário):** método explícito, não efeito colateral em `setCargo()` — consistente com o padrão de ação de domínio explícita já usado em `Maquina`/`Servico`.

**Depois:**
- `Usuario.derivarRoleDoCargo()` (void) — mesma regra (`cargo != null && !"ESTAGIARIO".equalsIgnoreCase(cargo)` → `"ADMIN"`, senão `"CLIENTE"`), mutando `this.role`.
- `Usuario.validarSenhaObrigatoria()` (void) — lança `IllegalArgumentException("Senha obrigatória para criar usuário")` se `senha` nula/em branco.
- `criarUsuario()`: `usuario.derivarRoleDoCargo(); usuario.validarSenhaObrigatoria(); usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));` — o `encode` fica no Service (depende do bean `PasswordEncoder`).
- `atualizarUsuario()`: mantém a guarda `if (usuarioAtualizado.getCargo() != null)` (decisão de *quando* atualizar continua no Service), mas dentro do bloco chama `existente.setCargo(...); existente.derivarRoleDoCargo();` em vez de recalcular o role inline.

### 4. `ItemAlmoxarifado.aplicarMovimentacao(String tipo, int quantidade)`

**Hoje** (`AlmoxarifadoService.registrarMovimentacao()`, linhas 64-66):
```java
int delta = "entrada".equalsIgnoreCase(dto.getTipo()) ? dto.getQuantidade() : -dto.getQuantidade();
item.setQuantidadeAtual(Math.max(0, item.getQuantidadeAtual() + delta));
```

**Depois:**
- `ItemAlmoxarifado.aplicarMovimentacao(String tipo, int quantidade)` (void) — mesmo cálculo de delta e mesmo piso em zero, mutando `this.quantidadeAtual`.
- `registrarMovimentacao()`: busca `item` via repository (fica no Service — precisa de `ItemAlmoxarifadoRepository`), depois `item.aplicarMovimentacao(dto.getTipo(), dto.getQuantidade()); itemRepository.save(item);`.

### 5. `KanbanCard.isVencido()`

**Hoje** (`DashboardEstagiarioService.getDashboard()`, linhas 51-56, dentro de um loop):
```java
if (c.getPrazo() != null && c.getPrazo().isBefore(LocalDate.now())
        && !"concluido".equalsIgnoreCase(c.getColuna())) {
    vencidas++;
}
```

**Depois:**
- `KanbanCard.isVencido()` (boolean) — mesma expressão, lendo `this.prazo`/`this.coluna`.
- Loop no Service: `if (c.isVencido()) { vencidas++; }`.
- Resto do método (`porColuna`, `taxaConclusao`, `mediaNota`, agregação cross-entity) **não muda** — é orquestração legítima entre `Estagiario`/`KanbanCard`/`NotaEstagiario` via 3 repositories, fora do escopo desta rodada.

## Fora de escopo desta rodada

- `DocumentoPDF.recalcularStatus()` — adiado até a Task 13g (unificação de `DocumentoPDF`) ser retomada.
- Todo o grupo "cópia de campos" (`atualizarDados()`/`normalizar()` em `Maquina`+filhos, `Edital`, `Projeto`, `Evento`, `Servico`, `Amostra`) — candidato a uma rodada 2, menos "regra de negócio" e mais DRY mecânico.
- Qualquer refactor de DTOs por caso de uso (Opção 3 / Case-Use DTOs) — só depois que Services virarem orquestradores de fato.

## Testes

Cada método de domínio ganha teste unitário direto na Entity, sem mocks (são funções puras sobre o próprio estado):
- `PastaDocumentoTest`: `validarExclusao()` não lança quando vazio; lança `IllegalStateException` com subpastas; lança com documentos.
- `DocumentoMaquinaTest`: `recalcularStatus()` para os 4 casos (`null`, expirado, dentro de 30 dias, ativo).
- `UsuarioTest`: `derivarRoleDoCargo()` para cargo nulo, "ESTAGIARIO" (case-insensitive) e outro cargo; `validarSenhaObrigatoria()` para nula/branca/válida.
- `ItemAlmoxarifadoTest`: `aplicarMovimentacao("entrada", n)` soma; `aplicarMovimentacao("saida", n)` subtrai; piso em zero quando saída excede o estoque.
- `KanbanCardTest`: `isVencido()` para prazo nulo, prazo passado com coluna "concluido" (falso), prazo passado com outra coluna (verdadeiro), prazo futuro (falso).

Ao final: `mvn compile` sem erros/warnings novos, e validação manual via `curl` dos endpoints tocados (`DELETE /api/pastas/{id}`, upload/listagem de `DocumentoMaquina`, `POST/PUT /api/usuarios`, `POST /api/almoxarifado/movimentacoes`, `GET` do dashboard de estagiários), confirmando que o comportamento observável não mudou.

## Critério de aceitação

- [ ] 5 métodos de domínio implementados nas Entities, cada um com teste unitário.
- [ ] Services correspondentes chamando os métodos, sem duplicar a regra.
- [ ] Nenhuma mudança de assinatura de endpoint ou de status HTTP de erro.
- [ ] `DocumentoPDF` intocado nesta rodada.
- [ ] Compilação sem erros/warnings novos.
