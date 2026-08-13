# Matriz de Impacto — Phase 1 (Unified Model Refactoring)

> Gerado para a Task 3 do plano em `docs/REFACTOR_PHASE1.md`. Todas as informações abaixo
> foram verificadas diretamente no código em
> `pilot/src/main/java/com/zeiss/pilot/{entity,dto,controller,service}` (não copiadas cegamente
> do texto do plano original). Onde o código real diverge do que o plano descreve, isso é
> sinalizado explicitamente na coluna "Relacionamentos/Dependências" ou em nota de rodapé.

## DTOs fora de escopo (NÃO serão deletados nesta Phase 1)

Estes DTOs não espelham uma única Entity 1:1 — são agregações/relatórios de leitura que
continuam existindo após a consolidação das 20 entities abaixo:

| DTO | Onde é usado | Motivo de ficar fora de escopo |
|---|---|---|
| `RelatorioMensalDTO` | `ServicoService.obterRelatorioMensal()` → `RelatorioController` (`GET /servicos/relatorio-servicos/api`) | Agregação (soma/contagem por ano+mês) sobre `Servico`, não é espelho de entity |
| `EventoRelatorioDTO` | `EventoService.getRelatoriosEventos()` → `RelatorioController` (`GET /eventos/relatorios`) | Agregação (totais, média de adesão, distribuição mensal) sobre `Evento` |
| `DashboardEstagiarioDTO` (+ nested `TotaisDTO`, `EstagiarioMetricasDTO`) | `DashboardEstagiarioService` → `DashboardEstagiarioController` | Agregação cross-entity: lê `Estagiario` + `KanbanCard` + `NotaEstagiario` (3 repositories injetados) e combina em métricas por estagiário |

Além destes, `PastaDocumentoDTO` (mirror de `PastaDocumento`, usado por `PastaDocumentoController`/`PastaDocumentoService`)
**também não está no escopo desta Phase 1** — `PastaDocumento` não consta nas 13 tasks de
consolidação (Tasks 4–13) do `REFACTOR_PHASE1.md`, embora seja referenciada por `DocumentoPDF`
(`@ManyToOne` obrigatório para `subpasta`). Fica como candidata a uma Phase 2, não deletar/alterar agora.

---

## Wave 2 — Simples (sem relações)

| Entity | DTO | Controller | Service | Relacionamentos/Dependências | Wave | Task # | Prioridade |
|---|---|---|---|---|---|---|---|
| `VisitaTecnica` | `VisitaTecnicaDTO` | `VisitaTecnicaController` | `VisitaTecnicaService` | Nenhuma (`entity/VisitaTecnica.java` não tem `@ManyToOne`/`@OneToMany`; nenhum outro controller/service importa `VisitaTecnicaDTO`) | 2 | 4 | P1 |
| `Projeto` | `ProjetoDTO` | `ProjetoController` | `ProjetoService` | **Real** `@ManyToOne private Usuario responsavel;` em `Projeto.java`. `ProjetoService` injeta `UsuarioRepository` diretamente (não `UsuarioDTO`/`UsuarioService`) para resolver `responsavelId`/`responsavelNome` (campos achatados no DTO). Como o acesso é via `UsuarioRepository`, a consolidação de `Projeto` **não depende** da consolidação de `Usuario` (Task 10) ter rodado antes. | 2 | 5 | P2 |
| `Edital` | `EditalDTO` | `EditalController` | `EditalService` | Nenhuma (`entity/Edital.java` sem relações; já tem `equals`/`hashCode`/`toString` — preservar ao consolidar) | 2 | 6 | P1 |
| `Estagiario` | `EstagiarioDTO` | `EstagiarioController` | `EstagiarioService` | Nenhuma relação JPA direta, mas é **lido** (via `EstagiarioRepository`) por `DashboardEstagiarioService` (fora de escopo) e referenciado por `Long estagiariaId`/`estagiariaId` solto em `KanbanCard` e `NotaEstagiario` (Task 13) — não é FK JPA, é campo simples. Atenção ao consolidar: manter o tipo `Long` desses campos-espelho intacto. | 2 | 7 | P2 |

**Checkpoint Wave 2:** 4 DTOs eliminados. `Projeto` e `Estagiario` têm dependência de leitura (P2); `VisitaTecnica` e `Edital` são P1 puros.

---

## Wave 3 — Moderado (com relações/particularidades)

| Entity | DTO | Controller | Service | Relacionamentos/Dependências | Wave | Task # | Prioridade |
|---|---|---|---|---|---|---|---|
| `Servico` | `ServicoDTO` | `ServicoController` | `ServicoService` | Nenhuma relação JPA. Particularidades reais confirmadas: (1) `ServicoController.listarServicos()` usa `Page<ServicoDTO>` com `@RequestParam query, status` (paginação + filtros) — preservar assinatura como `Page<Servico>`; (2) `ServicoService` já expõe `Servico.finalizar()` (comportamento de domínio da Task 2, já implementado); (3) `RelatorioMensalDTO` é gerado por `obterRelatorioMensal()` lendo `Servico` — **não deletar** esse DTO de relatório. | 3 | 8 | P3 |
| `Evento` | `EventoDTO` | `EventoController` | `EventoService` | Nenhuma relação JPA. Particularidade confirmada: `Evento.java` usa campo real `nome`; `EventoDTO.java` expõe `titulo` (com `fromEntity`/`toEntity` fazendo `dto.setTitulo(e.getNome())` e `e.setNome(this.titulo)`). Ao consolidar, manter `nome` como campo real e adicionar `getTitulo()`/`setTitulo()` delegando para `nome` (Opção B do plano). `EventoRelatorioDTO` é gerado por `EventoService.getRelatoriosEventos()` lendo `Evento` — **não deletar**. | 3 | 9 | P3 |
| `Usuario` | `UsuarioDTO` | `UsuarioController` | `UsuarioService` | **Divergência do plano identificada:** `entity/Usuario.java` **não possui**, hoje, nenhuma coleção `documentos`/`notas` (`@OneToMany`) — são apenas campos escalares (`nome`, `email`, `senha`, `role`, `cargo`, `dataCriacao`). O relacionamento pesado que existe de fato é o inverso: `DocumentoPDF` tem `@ManyToOne private Usuario usuario;` (obrigatório). Não há hoje nenhuma entity com `@ManyToOne`/coleção apontando "notas" para `Usuario` (as `NotaEstagiario` referenciam `Estagiario`/`Servico` via `Long`, não `Usuario`). Também é referenciada por `Projeto.responsavel` (`@ManyToOne`, ver Wave 2). Ao implementar a Task 10, o flag de `toDTO(boolean incluirRelacionados)` citado no plano precisará ser construído (ex.: via `DocumentoPDFRepository`), pois a coleção inversa ainda não existe na Entity — **verificar com o time antes de inventar um `@OneToMany` novo em `Usuario`** só para satisfazer essa assinatura. | 3 | 10 | P3 |

**Checkpoint Wave 3:** mais 3 DTOs eliminados. `Servico`/`Evento` = P3 por causa de paginação/alias, respectivamente. `Usuario` = P3 pois é referenciado por `Projeto` e `DocumentoPDF`, mas a "particularidade" descrita no plano (documentos/notas) não bate com a Entity atual — necessita decisão de design antes de codar.

---

## Wave 4 — Complexo

### Agregado `Maquina` (Task 11)

| Entity | DTO | Controller | Service | Relacionamentos/Dependências | Wave | Task # | Prioridade |
|---|---|---|---|---|---|---|---|
| `Maquina` (raiz) | `MaquinaDTO` | `MaquinaController` | `MaquinaService` | Hoje **sem** `@OneToMany` inversos em `Maquina.java` (só campos escalares + `ligar()`/`desligar()`, já implementados na Task 2). As relações existem apenas no lado filho: `SessaoMaquina.maquina`, `ManutencaoMaquina.maquina`, `AgendamentoMaquina.maquina` e `DocumentoMaquina.maquina` são todas `@ManyToOne(fetch = LAZY)`. `MaquinaController`/`MaquinaService` **já implementam o padrão de endpoints aninhados (Opção B)** descrito no plano: `/api/maquinas/{id}/sessoes`, `/{id}/manutencoes`, `/{id}/agendamentos` (GET/POST/DELETE), todos hoje devolvendo `*DTO`. Ao consolidar, adicionar `@OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)` em `Maquina` para `sessoes`/`manutencoes`/`agendamentos` conforme pedido pela Task 11. | 4 | 11 | P4 |
| `SessaoMaquina` | `SessaoMaquinaDTO` | *(nenhum controller próprio — endpoints em `MaquinaController`)* | *(nenhum service próprio — métodos em `MaquinaService`)* | `@ManyToOne(fetch = LAZY) private Maquina maquina;` (FK real). DTO usa `maquinaId` achatado. | 4 | 11 | P4 |
| `ManutencaoMaquina` | `ManutencaoMaquinaDTO` | *(idem — `MaquinaController`)* | *(idem — `MaquinaService`)* | `@ManyToOne(fetch = LAZY) private Maquina maquina;`. DTO usa `maquinaId` achatado. | 4 | 11 | P4 |
| `AgendamentoMaquina` | `AgendamentoMaquinaDTO` | *(idem — `MaquinaController`)* | *(idem — `MaquinaService`)* | `@ManyToOne(fetch = LAZY) private Maquina maquina;`. DTO usa `maquinaId` achatado. | 4 | 11 | P4 |

> Nota: diferente do que a estrutura de pastas do plano sugere, **não existem** `SessaoMaquinaController`/`SessaoMaquinaService`,
> `ManutencaoMaquinaController`/`ManutencaoMaquinaService` nem `AgendamentoMaquinaController`/`AgendamentoMaquinaService` como
> classes separadas — tudo está centralizado em `MaquinaController`/`MaquinaService`. A consolidação das 4 entities do
> agregado, portanto, acontece dentro desses 2 únicos arquivos (mais os 4 arquivos de `entity/`).

### `Amostra` (Task 12)

| Entity | DTO | Controller | Service | Relacionamentos/Dependências | Wave | Task # | Prioridade |
|---|---|---|---|---|---|---|---|
| `Amostra` | `AmostraDTO` | `AmostraController` | `AmostraService` | Nenhuma relação JPA (`@ManyToOne`/`@OneToMany`) — é uma entity "documento/formulário" grande e plana (10 seções, ~80 campos). Único relacionamento estrutural é `@ElementCollection private List<String> servicos` (tabela auxiliar `amostra_servicos`, não é outra Entity). Campos de condição (`condPecaConforme`, `condQuantidadeCorreta`, `condEmbalagemIntegra`, `condSemDanoTransporte`, `condPecaLimpa`, `condSemContaminacao`, `condIdentificacao`, `condDocumentos`, `condPermiteExecucao`) são todos `String` individuais hoje — **não há** um método auxiliar existente que os exponha como `Map` (o plano pede para "preservar" tal método, mas ele ainda não existe no código; será necessário criar, não apenas preservar). | 4 | 12 | P4 |

### Restantes (Task 13)

| Entity | DTO | Controller | Service | Relacionamentos/Dependências | Wave | Task # | Prioridade |
|---|---|---|---|---|---|---|---|
| `DocumentoMaquina` | `DocumentoMaquinaDTO` | `DocumentoMaquinaController` | `DocumentoMaquinaService` | `@ManyToOne(fetch = LAZY) private Maquina maquina;` (FK real). DTO usa `maquinaId` achatado. Depende do agregado `Maquina` (Task 11) apenas para a referência à Entity `Maquina`, não ao DTO. | 4 | 13 | P3 |
| `Avaliacao` | `AvaliacaoDTO` | `AvaliacaoController` | `AvaliacaoService` | Nenhuma relação JPA (campos `realizouServico`/`descServico` são texto livre, não FK). | 4 | 13 | P1 |
| `NotaEstagiario` | `NotaEstagiarioDTO` | `NotaEstagiarioController` | `NotaEstagiarioService` | Sem `@ManyToOne` JPA — `estagiariaId` e `servicoId` são `Long` soltos (referência lógica, não FK mapeada). **Lida** por `DashboardEstagiarioService` (fora de escopo) via `NotaEstagiarioRepository.findByEstagiariaId(...)`. | 4 | 13 | P2 |
| `KanbanCard` | `KanbanCardDTO` | `KanbanCardController` | `KanbanCardService` | Sem `@ManyToOne` JPA — `estagiariaId` é `Long` solto. Tem `@ElementCollection private List<String> tags` (tabela auxiliar `kanban_card_tags`). **Lida** por `DashboardEstagiarioService` (fora de escopo) via `KanbanCardRepository.findByEstagiariaId(...)`. | 4 | 13 | P2 |
| `ItemAlmoxarifado` | `ItemAlmoxarifadoDTO` | `AlmoxarifadoController` (endpoints `/api/almoxarifado/itens/**`) | `AlmoxarifadoService` (método `salvarItem`/`atualizarItem`/`deletarItem`/`listarItens`) | É o lado "pai" de `MovimentacaoAlmoxarifado` (`@ManyToOne(fetch = LAZY) private ItemAlmoxarifado item;` em `MovimentacaoAlmoxarifado.java`), mas a própria `ItemAlmoxarifado` não tem `@OneToMany` inverso. **Não existe** `ItemAlmoxarifadoController`/`ItemAlmoxarifadoService` dedicados — tudo está em `AlmoxarifadoController`/`AlmoxarifadoService`, compartilhado com `MovimentacaoAlmoxarifado`. | 4 | 13 | P2 |
| `MovimentacaoAlmoxarifado` | `MovimentacaoAlmoxarifadoDTO` | `AlmoxarifadoController` (endpoints `/api/almoxarifado/movimentacoes/**`) | `AlmoxarifadoService` (métodos `registrarMovimentacao`/`listarMovimentacoesPorItem`) | `@ManyToOne(fetch = LAZY) private ItemAlmoxarifado item;` (FK real). DTO deve ter `itemId` achatado — consolidar junto com `ItemAlmoxarifado` no mesmo controller/service, pois ambos compartilham as mesmas classes. | 4 | 13 | P2 |
| `VerificacaoAmbiental` | `VerificacaoAmbientalDTO` | `VerificacaoAmbientalController` | `VerificacaoAmbientalService` | Nenhuma relação JPA (todos os campos são escalares: `Double`, `Boolean`, `String`). | 4 | 13 | P1 |
| `DocumentoPDF` | `DocumentoPDFDTO` | `DocumentoPDFController` | `DocumentoPDFService` | Duas relações JPA reais: `@ManyToOne private Usuario usuario;` (obrigatório) e `@ManyToOne(optional = false) private PastaDocumento subpasta;` (obrigatório). Caso especial confirmado no DTO: `DocumentoPDFDTO` tem campos de compatibilidade `pastaId`/`nomePasta` (comentado no código como "pasta principal (pastaPai da subpasta)") além dos campos corretos `subpastaId`/`nomeSubpasta` e `usuarioId`/`usuarioRole`. Conforme o plano, remover `pastaId`/`nomePasta` na consolidação e usar a relação real `subpasta` → `subpasta.getPastaPai()` quando necessário. Depende de `PastaDocumento` (fora de escopo desta Phase 1) permanecer estável. | 4 | 13 | P4 |

**Checkpoint Wave 4:** 8 DTOs eliminados nesta tabela + 4 do agregado `Maquina` + 1 de `Amostra` = 13 DTOs, fechando as 20 entities / 13 tasks do plano.

---

## Resumo de prioridades

| Prioridade | Critério | Entities |
|---|---|---|
| P1 | Simples, sem relações | `VisitaTecnica`, `Edital`, `Avaliacao`, `VerificacaoAmbiental` |
| P2 | Simples, com dependência de leitura (outro serviço lê via repository, ou tem FK mas sem lógica extra) | `Projeto`, `Estagiario`, `NotaEstagiario`, `KanbanCard`, `ItemAlmoxarifado`, `MovimentacaoAlmoxarifado`, `DocumentoMaquina` |
| P3 | Moderado, com relações e/ou particularidades de contrato (paginação, alias de campo, flags) | `Servico`, `Evento`, `Usuario` |
| P4 | Agregação complexa (raiz + filhos, ou entity grande com muitos campos/particularidades) | `Maquina`, `SessaoMaquina`, `ManutencaoMaquina`, `AgendamentoMaquina`, `Amostra`, `DocumentoPDF` |

Total: 20 entities cobertas nas Tasks 4–13 (13 tasks, sendo a Task 11 = 4 entities e a Task 13 = 8 entities).
