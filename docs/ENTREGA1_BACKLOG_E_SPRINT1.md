# Entrega 1 — Backlog de Tarefas Restantes e Sprint 1

**Projeto:** Zeiss-Pilot (CEM SENAI) — Sistema Web para Gestão e Rastreabilidade do Laboratório de Metrologia
**Entrega/Apresentação da Sprint 1:** 20/08/2026
**Equipe:** João Vítor Mamede, Gabriel Viana Nunes

---

## 1. Contexto

O sistema já está funcional e cobre os módulos descritos no documento de TCC (Serviços,
Máquinas, Amostras, Almoxarifado, Estagiários, Visitas Técnicas, Verificação Ambiental,
Editais/Projetos/Eventos, Avaliações/NPS, Documentos e Usuários), com backend em Java 21 +
Spring Boot 3.4 e frontend em Thymeleaf/HTML/CSS/JS.

Em paralelo ao desenvolvimento de funcionalidades, a equipe conduziu um refactor interno de
modelo de dados (unificação de Entities/DTOs e introdução de comportamento de domínio nas
Entities), concluído e revisado, mas ainda não integrado à branch principal. Este documento
lista o que resta para fechar esse ciclo e alinhar o sistema aos requisitos descritos no TCC,
e define o recorte que entra na Sprint 1.

Este documento cobre apenas o trabalho de integração/fechamento do que já está em andamento
e as divergências pontuais encontradas entre o TCC e o código atual — não inclui novas
funcionalidades de produto (dashboards avançados, relatórios em PDF, módulo de calibração,
notificações automáticas, app mobile), que ficam registradas como trabalhos futuros do TCC e
fora do escopo desta rodada.

---

## 2. Backlog Completo

### Epic A — Integração e fechamento do refactor em andamento

| # | Item | Descrição | Status atual |
|---|------|-----------|---------------|
| A1 | Merge da branch `refactor/unified-model-phase1` para a branch principal | Phase 1 (unificação de 19/20 Entities+DTOs) e Phase 2 Rodada 1 (comportamento de domínio em 5 Entities) estão implementadas, testadas (suíte completa + regressão manual via curl) e com revisão final de branch aprovada. Falta apenas o merge. | Pronto para integrar |
| A2 | Retomar unificação de `DocumentoPDF` (Task 13g da Phase 1) | Foi deliberadamente adiada em 2026-07-01 porque a equipe estava desenvolvendo ativamente a lógica de documentos nessa área. Deve ser retomada quando esse desenvolvimento for concluído. | Em andamento (fora desta sprint) |
| A3 | Phase 2 Rodada 2 — candidatos mecânicos de domínio adiados da Rodada 1 (normalização de campos em `Maquina`/`SessaoMaquina`/`ManutencaoMaquina`/`AgendamentoMaquina`, `Edital`, `Projeto`, `Evento`, `Servico.normalizar()`, `Amostra.normalizar()`) | Identificados na Rodada 1 como candidatos de menor prioridade (cópia/normalização de campos, não regra de negócio). Ainda não planejados em detalhe. | Não iniciado |
| A4 | Fase de Case-Use DTOs por endpoint | Decisão já tomada: só começa depois que o comportamento de domínio nas Entities estiver estável (evita remapear tudo de novo). Ainda não iniciada. | Não iniciado |

### Epic B — Correções de aderência ao TCC

| # | Item | Descrição | Status atual |
|---|------|-----------|---------------|
| B1 | Restaurar a criação automática do usuário administrador (`AdminInitializer`) | A classe está com o corpo comentado — hoje não existe seed automático de admin na inicialização, exigindo acesso manual ao banco para o primeiro login. O TCC (seção 10, Padrões de Projeto) descreve essa inicialização automática como existente. | Pendente |
| B2 | Implementar o papel `TÉCNICO` no controle de acesso | O TCC especifica 3 perfis (RF11, RNF02, Especificação de Interface): `ADMIN`, `TÉCNICO`, `CLIENTE`. O código hoje só deriva `ADMIN` ou `CLIENTE` a partir do cargo (`Usuario.derivarRoleDoCargo()`), sem um terceiro perfil intermediário. | Pendente |

---

## 3. Sprint 1 (04/08/2026 → 20/08/2026)

Time trabalha de forma sequencial (um item concluído antes de iniciar o próximo). Ordem de
prioridade definida pela equipe:

1. **A1 — Merge do refactor para a branch principal.** Prioridade máxima: é trabalho já
   pronto, testado e revisado — travá-lo mais tempo sem integrar só aumenta o risco de
   conflito com desenvolvimento futuro. Critério de pronto: branch principal atualizada,
   build e suíte de testes passando na branch principal.
2. **B1 — Restaurar `AdminInitializer`.** Pequeno e rápido, remove a necessidade de acesso
   manual ao banco para obter um usuário administrador. Critério de pronto: aplicação subindo
   do zero (banco limpo) já permite login como ADMIN sem intervenção manual.
3. **B2 — Papel `TÉCNICO`.** Se houver tempo após os itens 1 e 2: fechar a lacuna entre o TCC
   e o código adicionando o terceiro perfil de acesso. Critério de pronto: `Usuario` pode
   assumir o papel `TÉCNICO`, e ao menos uma regra de autorização (`SecurityConfig` ou
   `@PreAuthorize`) diferencia esse perfil de `CLIENTE`.

Itens A2, A3, A4 permanecem no backlog e não fazem parte desta sprint.

---

## 4. Evidência no Trello

*(Espaço reservado — anexar aqui o print do quadro do Trello mostrando os itens A1, B1 e B2
planejados para a Sprint 1.)*

---

## 5. Versão pronta para colar no Trello

Listas do quadro (ordem definida pelo professor): **Backlog** · **Em Andamento** ·
**Testando** · **Pronto**. Como a Sprint 1 ainda não começou, todos os cards abaixo entram
na lista **Backlog** — use uma etiqueta (label) "Sprint 1" nos cards A1, B1 e B2 para
diferenciá-los dos demais itens do backlog geral (A2, A3, A4), e mova cada card para
Em Andamento → Testando → Pronto conforme o trabalho avançar ao longo da sprint. Um card
por item abaixo — título na primeira linha, descrição no restante.

### Card A1

```
Merge do refactor unified-model-phase1 para a branch principal

Phase 1 (unificação de 19/20 Entities+DTOs) e Phase 2 Rodada 1 (comportamento de
domínio em 5 Entities) estão implementadas, testadas (suíte completa + regressão
manual via curl) e com revisão final de branch aprovada.

Falta apenas integrar a branch `refactor/unified-model-phase1` na branch principal.

Critério de pronto: branch principal atualizada, build e suíte de testes passando.

Status: pronto para integrar | Prioridade: 1 (Sprint 1)
```

### Card A2

```
Retomar unificação de DocumentoPDF (Task 13g)

Adiada deliberadamente em 2026-07-01 porque a equipe estava desenvolvendo
ativamente a lógica de documentos nessa área. Retomar quando esse trabalho for
concluído.

Status: em andamento (fora da Sprint 1)
```

### Card A3

```
Phase 2 Rodada 2 — normalização de campos (candidatos mecânicos)

Candidatos identificados na Rodada 1 mas de menor prioridade (cópia/normalização
de campos, não regra de negócio): Maquina/SessaoMaquina/ManutencaoMaquina/
AgendamentoMaquina, Edital, Projeto, Evento, Servico.normalizar(),
Amostra.normalizar().

Status: não iniciado | Backlog (fora da Sprint 1)
```

### Card A4

```
Fase de Case-Use DTOs por endpoint

Decisão já tomada: só começa depois que o comportamento de domínio nas Entities
estiver estável, para evitar remapear tudo de novo.

Status: não iniciado | Backlog (fora da Sprint 1)
```

### Card B1

```
Restaurar a criação automática do usuário administrador (AdminInitializer)

A classe está com o corpo comentado — hoje não existe seed automático de admin
na inicialização da aplicação, exigindo acesso manual ao banco para o primeiro
login.

Critério de pronto: aplicação subindo do zero (banco limpo) já permite login
como ADMIN sem intervenção manual.

Status: pendente | Prioridade: 2 (Sprint 1)
```

### Card B2

```
Implementar o papel TÉCNICO no controle de acesso

O sistema especifica 3 perfis (ADMIN, TÉCNICO, CLIENTE). O código hoje só
deriva ADMIN ou CLIENTE a partir do cargo do usuário, sem um terceiro perfil
intermediário.

Critério de pronto: Usuario pode assumir o papel TÉCNICO, e ao menos uma regra
de autorização diferencia esse perfil de CLIENTE.

Status: pendente | Prioridade: 3 (Sprint 1, se houver tempo)
```
