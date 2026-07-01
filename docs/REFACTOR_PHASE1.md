# Phase 1: Unified Model Refactoring

**Status:** Em execução
**Branch:** refactor/unified-model-phase1
**Objetivo:** Eliminar DTOs espelho (Entity + DTO idênticos) e consolidar em uma única classe (Entity com `toDTO()`/`fromDTO()` quando necessário para compatibilidade, mas Controllers/Services passam a trabalhar com a Entity diretamente).

Reconstruído a partir do plano original (colado em duas partes pelo usuário em 2026-06-30). Onde o texto original dizia "padrão repetido", os detalhes foram preenchidos consultando o código real do projeto (`pilot/src/main/java/com/zeiss/pilot`).

## Padrão geral (aplicado em todas as tasks de consolidação)

**Decisão (pós-Task 4):** a Entity NÃO recebe métodos `fromDTO()`/`toDTO()` referenciando a classe DTO quando nada mais no código consumirá essa conversão depois da migração — criar esses métodos exigiria manter uma versão morta do DTO só para compilar, o que contradiz o objetivo de "0 DTOs espelho restantes". Passo 1 abaixo só se aplica nos casos raros em que outro componente (ex.: um relatório de leitura) realmente precisa de uma conversão Entity→DTO após a consolidação; isso é decidido task a task, não por padrão.

Para cada Entity a consolidar:

1. **Entity (`*.java`)**: normalmente nenhuma mudança estrutural é necessária além de comportamentos de domínio já cobertos na Task 2. Só adicionar um método de conversão se uma task específica identificar um consumidor real que precise dele (avaliar caso a caso).
2. **Controller**: trocar `ResponseEntity<List<EntityDTO>>` / `ResponseEntity<EntityDTO>` por `ResponseEntity<List<Entity>>` / `ResponseEntity<Entity>`; parâmetros `@RequestBody EntityDTO` viram `@RequestBody Entity`; remover chamadas `.toDTO()`/`.fromDTO()`/`::fromEntity` do controller.
3. **Service**: remover conversões `.map(EntityDTO::fromEntity)` / `.toDTO()`; retornar a Entity diretamente do repository. Remover métodos que só existiam para trabalhar com o DTO e não têm chamadores.
4. **Deletar** `dto/EntityDTO.java`, e buscar em todo `pilot/src/main/java` por outras referências ao DTO (outros controllers/services) que precisem ser atualizadas.
5. **Validar**: compilar (`mvn compile`). O teste funcional (subir a aplicação e testar com `curl` os endpoints alterados) é feito pelo controlador da refatoração uma vez ao final de cada Wave, não por task individual.

---

## Wave 1 — Preparação & Análise

### Task 1: Criar `MapperUtil`
Criar `pilot/src/main/java/com/zeiss/pilot/util/MapperUtil.java` com utilitário de mapping genérico reutilizável (ex.: `copyNonNullProperties(source, target)` usando `BeanUtils.copyProperties` + `getNullPropertyNames`). Serve de apoio às tasks de consolidação seguintes (não é obrigatório usá-lo em todas, mas deve estar disponível).

**Aceitação:** classe criada, compila.

### Task 2: Comportamentos de domínio explicitados no plano
Adicionar às Entities os comportamentos de domínio já especificados no plano original (não inventar novos):
- `Maquina.ligar()`: lança `IllegalStateException` se `status` for `"Manutenção"`, senão marca `ligada = true`.
- `Maquina.desligar()`: marca `ligada = false`, sem validação.
- `Servico` (finalizar): lança `IllegalStateException` se `status` contiver `"pendente"`, senão define `status = "Finalizado"`.

Essas são as únicas 3 assinaturas de comportamento presentes no texto original; nenhuma outra Entity teve comportamento especificado, então nenhuma outra recebe método novo nesta task (evitar inventar regras de negócio não pedidas).

**Aceitação:** métodos compilam; nenhuma mudança de contrato de API ainda.

### Task 3: Matriz de Impacto
Criar `docs/REFACTOR_IMPACT_MATRIX.md` documentando, para cada Entity a consolidar: DTO correspondente, Controller, Service, dependências/relacionamentos com outras Entities ou DTOs de leitura (ex.: `RelatorioMensalDTO`, `EventoRelatorioDTO`, `DashboardEstagiarioDTO` — esses ficam, pois são DTOs de relatório/agregação, não espelhos de Entity), prioridade (P1 simples → P4 agregação complexa).

**Aceitação:** matriz preenchida cobrindo as 13 tasks de consolidação abaixo.

---

## Wave 2 — Simples (sem relações)

### Task 4: Unificar `VisitaTecnica`
- `entity/VisitaTecnica.java`, `controller/VisitaTecnicaController.java`, `service/VisitaTecnicaService.java`
- Deletar `dto/VisitaTecnicaDTO.java`
- Padrão geral acima.

### Task 5: Unificar `Projeto`
- `entity/Projeto.java`, `controller/ProjetoController.java`, `service/ProjetoService.java`
- Deletar `dto/ProjetoDTO.java`
- Padrão idêntico à Task 4.

### Task 6: Unificar `Edital`
- `entity/Edital.java`, `controller/EditalController.java`, `service/EditalService.java`
- Deletar `dto/EditalDTO.java`
- Padrão idêntico à Task 4.

### Task 7: Unificar `Estagiario`
- `entity/Estagiario.java`, `controller/EstagiarioController.java`, `service/EstagiarioService.java`
- Deletar `dto/EstagiarioDTO.java`
- Padrão idêntico à Task 4.

**Checkpoint Wave 2:** 4 DTOs eliminados (~400 linhas).

---

## Wave 3 — Moderado (com relações/particularidades)

### Task 8: Unificar `Servico`
- `entity/Servico.java`, `controller/ServicoController.java`, `service/ServicoService.java`
- Deletar `dto/ServicoDTO.java`
- Particularidade: paginação (`Page<Servico>` em vez de `Page<ServicoDTO>`) e filtros (`query`, `status`) preservados na assinatura do Service/Controller.
- `RelatorioMensalDTO` continua existindo (lê de `Servico`, não é espelho — não deletar).

### Task 9: Unificar `Evento`
- `entity/Evento.java`, `controller/EventoController.java`, `service/EventoService.java`
- Deletar `dto/EventoDTO.java`
- Particularidade (campo confuso `titulo` ↔ `nome`): manter `nome` como campo real e adicionar aliases `getTitulo()`/`setTitulo()` delegando para `nome`, para o frontend continuar funcionando sem alteração (Opção B do plano original, já recomendada).
- `EventoRelatorioDTO` continua existindo (leitura) — não deletar.

### Task 10: Unificar `Usuario`
- `entity/Usuario.java`, `controller/UsuarioController.java`, `service/UsuarioService.java`
- Deletar `dto/UsuarioDTO.java`
- Particularidade: `toDTO()` deve continuar existindo com uma sobrecarga/flag para controlar se inclui relacionamentos pesados (`documentos`, `notas`) — por padrão NÃO incluir, para não sobrecarregar respostas de listagem.

**Checkpoint Wave 3:** mais 3 DTOs eliminados, padrão com relações validado.

---

## Wave 4 — Complexo

### Task 11: Unificar `Maquina` (raiz de agregação)
Estrutura agregada:
```
Maquina (raiz)
├── SessaoMaquina (1:N)
├── ManutencaoMaquina (1:N)
└── AgendamentoMaquina (1:N)
```
- Consolidar `Maquina`, `SessaoMaquina`, `ManutencaoMaquina`, `AgendamentoMaquina` (4 DTOs deletados).
- Decisão já tomada no plano original: **Opção B** — endpoints aninhados RESTful (`/api/maquinas/{id}/sessoes`, `/api/maquinas/{id}/manutencoes`, etc.) em vez de aninhar tudo dentro do JSON de `Maquina`.
- Implementar `Maquina.ligar()`/`desligar()` (ver Task 2) usando as coleções `sessoes`/`manutencoes` via `@OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)`.

### Task 12: Unificar `Amostra`
- `entity/Amostra.java`, `controller/AmostraController.java`, `service/AmostraService.java`
- Deletar `dto/AmostraDTO.java`
- Particularidade: campos de condição (`condPecaConforme`, `condQuantidadeCorreta`, etc.) — preservar qualquer método auxiliar que os exponha como `Map` (ex. para relatório), usando `@PostLoad` se necessário.

### Task 13: Unificar restantes
Entities: `DocumentoMaquina`, `Avaliacao`, `NotaEstagiario`, `KanbanCard`, `ItemAlmoxarifado`, `MovimentacaoAlmoxarifado`, `VerificacaoAmbiental`, `DocumentoPDF`.
- Padrão geral (Task 4), aplicado individualmente a cada uma.
- Caso especial `DocumentoPDF`: remover campos `pastaId`, `nomePasta` do DTO (eram de compatibilidade; com Entity unificada, usar a relação real com `PastaDocumento`).

---

## Verificação Final da Phase 1
- [ ] 0 DTOs espelho restantes (DTOs de relatório/agregação como `RelatorioMensalDTO`, `EventoRelatorioDTO`, `DashboardEstagiarioDTO` são mantidos)
- [ ] Todas as Entities consolidadas com `toDTO()`/`fromDTO()`
- [ ] Controllers e Services retornam Entities diretamente
- [ ] Compilação sem erros/warnings novos
- [ ] Regressão manual (curl) sem quebras nos endpoints alterados
