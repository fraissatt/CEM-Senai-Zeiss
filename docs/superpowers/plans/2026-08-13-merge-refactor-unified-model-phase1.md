# Merge `refactor/unified-model-phase1` → `main` — Plano de Verificação e Integração

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Confirmar, com evidência reproduzível, que a branch `refactor/unified-model-phase1` (Phase 1 + Phase 2 Rodada 1, já implementadas e revisadas) pode ser integrada à `main` sem regressão, e executar essa integração.

**Architecture:** Este não é um plano de desenvolvimento de feature nova — é um plano de verificação. Cada task reexecuta uma camada de evidência já produzida anteriormente na branch (suíte automatizada, regressão manual via curl, checagem visual no navegador) para confirmar que nada mudou desde a última verificação, depois executa o merge e repete a verificação mínima na própria `main`.

**Tech Stack:** Java 21, Spring Boot 3.4, Maven, PostgreSQL local (`senai_zeiss`), Git.

## Global Constraints

- Nenhuma alteração de código de negócio nesta rodada — este plano só testa e integra o que já existe na branch. Itens A2, A3, A4, B1, B2 (ver `docs/ENTREGA1_BACKLOG_E_SPRINT1.md`) estão fora de escopo.
- Se qualquer step falhar, PARE — não pule para o próximo step nem "contorne" o problema. Diagnostique a causa raiz antes de prosseguir (usar superpowers:systematic-debugging se a causa não for óbvia).
- No Windows, usar `mvn` do PATH (`C:\Apache\Maven\apache-maven-3.8.8\bin`), nunca `.\mvnw.cmd` — o wrapper quebra com espaço no caminho (`Zeiss-Pilot`/`João Vítor Mamede`), bug conhecido e já documentado em `.superpowers/sdd/progress.md`.
- Login de teste ADMIN já existe no Postgres local: `admin@zeiss.com` / `SENHA_DE_TESTE` (setado via `psql` porque `AdminInitializer` está comentado — item B1 do backlog, fora de escopo aqui).
- Remote correto para push é `integration` (não `origin`, que aponta pro fork de um colega).
- Toda evidência de execução (saída de comando, status HTTP, prints) deve ser registrada no `.superpowers/sdd/progress.md` ao final de cada task, seguindo o padrão já usado nas rodadas anteriores.

---

### Task 1: Pré-voo — confirmar zero desvio entre `main` e a branch

**Files:** nenhum arquivo alterado — apenas comandos git.

**Interfaces:**
- Produces: confirmação de que `main` não recebeu nenhum commit desde que a branch foi criada (pré-condição para os merges seguintes serem um fast-forward limpo, sem conflito).

- [ ] **Step 1: Verificar working tree limpa**

Run: `git status --porcelain`
Expected: saída vazia (nenhuma alteração não commitada). Se houver algo, pare e decida com o usuário se deve ser commitado, descartado ou é trabalho em progresso — não prossiga sem resolver.

- [ ] **Step 2: Confirmar que `main` não avançou desde a divergência**

Run: `git log --oneline refactor/unified-model-phase1..main`
Expected: saída vazia (zero commits). Confirmado anteriormente em 2026-08-13: merge-base = tip de `main` (`14f5b39`), 36 commits só na branch, 0 só na `main`.

Se a saída **não** estiver vazia (alguém commitou direto em `main` desde então), PARE — este plano assume merge sem conflito; conflito real exige uma sessão de resolução própria, não coberta aqui.

- [ ] **Step 3: Registrar contagem de commits da branch**

Run: `git log --oneline main..refactor/unified-model-phase1 | wc -l`
Expected: `36` (ou mais, se algo novo foi commitado na branch depois de 2026-08-13 — nesse caso ajuste a expectativa e prossiga).

---

### Task 2: Suíte de testes automatizada na branch

**Files:** nenhum arquivo alterado.

**Interfaces:**
- Consumes: branch `refactor/unified-model-phase1` no estado do Step 1.
- Produces: confirmação "BUILD SUCCESS" com 0 failures/errors — pré-requisito para a Task 4 (não faz sentido testar manualmente uma branch com suíte quebrada).

- [ ] **Step 1: Rodar a suíte completa**

Run: `cd pilot && mvn test`
Expected: `BUILD SUCCESS` ao final, linha `Tests run: N, Failures: 0, Errors: 0, Skipped: 0`.

- [ ] **Step 2: Se falhar, não prosseguir**

Se `BUILD FAILURE`: pare o plano aqui, identifique a causa (branch pode ter divergido do último estado verificado em `48577d9`/`2f57c2c` — checar `git log` desde esses commits) e corrija antes de continuar para a Task 3.

---

### Task 3: Empacotar o artefato

**Files:** nenhum arquivo alterado — gera `pilot/target/pilot-0.0.1-SNAPSHOT.jar`.

**Interfaces:**
- Consumes: suíte verde da Task 2.
- Produces: JAR executável, usado na Task 4 para subir o servidor local.

- [ ] **Step 1: Empacotar sem re-rodar os testes** (já rodaram na Task 2, evita duplicar tempo)

Run: `cd pilot && mvn package -DskipTests`
Expected: `BUILD SUCCESS`, arquivo `pilot/target/pilot-0.0.1-SNAPSHOT.jar` criado.

---

### Task 4: Regressão manual via curl — endpoints de maior risco

**Files:** nenhum arquivo alterado.

**Interfaces:**
- Consumes: servidor local rodando (`mvn spring-boot:run`), login ADMIN (`admin@zeiss.com` / `SENHA_DE_TESTE`).
- Produces: confirmação de que os pontos que já tiveram bug real encontrado e corrigido durante o refactor continuam corretos (não é uma re-execução às cegas — é checar exatamente onde já quebrou antes).

Este é um subconjunto direcionado do checklist de 10 endpoints já rodado duas vezes (Wave 4 e Phase 2 Rodada 1) — não repete tudo, foca nos pontos que tiveram bug real:

| Achado original | Endpoint a reconfirmar | O que checar |
|---|---|---|
| `Usuario.senha` vazava em JSON aninhado | `GET /api/projetos` | Campo `responsavel{}` aparece aninhado, sem `senha` |
| Proxy LAZY do Hibernate quebrava Jackson (500) | `GET /api/almoxarifado/movimentacoes` | Retorna 200, `item{}` aninhado corretamente |
| `AmostraDTO` aninhava campos que a Entity não tem | `GET /api/amostras` | Campos `cond*`/`foto*` **planos**, não aninhados em `condicao{}`/`fotos{}` |
| `isVencido()` vazou como campo novo no JSON | `GET /api/kanban-cards` | JSON **não** contém o campo `vencido` |
| `@JsonIgnore` no back-reference de documentos | `GET /api/maquinas/1/documentos` | Retorna 200, sem referência circular a `maquina{}` |

- [ ] **Step 1: Subir o servidor**

Run (background): `cd pilot && mvn spring-boot:run`
Expected: log mostra `Started PilotApplication` e porta 8080 ativa.

- [ ] **Step 2: Autenticar e capturar cookie de sessão + CSRF**

```bash
curl -c cookies.txt http://localhost:8080/login | grep -o 'name="_csrf" value="[^"]*"'
CSRF=$(curl -c cookies.txt -s http://localhost:8080/login | grep -oP '(?<=name="_csrf" value=")[^"]*')
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/login \
  -d "username=admin@zeiss.com&password=SENHA_DE_TESTE&_csrf=$CSRF" -i | head -20
```
Expected: resposta `302 Found` com `Location: /index` (login bem-sucedido).

- [ ] **Step 3: Rodar os 5 checks da tabela acima**

```bash
curl -b cookies.txt -s http://localhost:8080/api/projetos | grep -o '"responsavel":{[^}]*}' | head -1
curl -b cookies.txt -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/almoxarifado/movimentacoes
curl -b cookies.txt -s http://localhost:8080/api/amostras | head -c 500
curl -b cookies.txt -s http://localhost:8080/api/kanban-cards | grep -c '"vencido"'
curl -b cookies.txt -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/maquinas/1/documentos
```
Expected, respectivamente: `responsavel{}` sem `"senha"`; `200`; campos `cond`/`foto` sem prefixo aninhado; `0` ocorrências de `"vencido"`; `200`.

- [ ] **Step 4: Parar o servidor**

Encerrar o processo `mvn spring-boot:run` (Ctrl+C ou kill do PID).

---

### Task 5: Checagem visual no navegador

**Files:** nenhum arquivo alterado.

**Interfaces:**
- Consumes: servidor local rodando novamente, login ADMIN.
- Produces: confirmação de que o frontend (JS que lê os campos checados na Task 4) renderiza sem erro — curl confirma o formato do JSON, mas só o navegador confirma que o JS que consome esse JSON não quebrou.

- [ ] **Step 1: Subir o servidor** (mesmo comando da Task 4, Step 1)

- [ ] **Step 2: Login via navegador**

Acessar `http://localhost:8080/login`, autenticar com `admin@zeiss.com` / `SENHA_DE_TESTE`.

- [ ] **Step 3: Visitar as páginas que consomem os campos alterados pelo refactor**

| Página | URL | O que olhar |
|---|---|---|
| Projetos | `/projetos` | Nome do responsável aparece corretamente (não "undefined") |
| Amostras | `/amostras` | Condição/fotos da amostra renderizam |
| Máquinas | `/maquinas` | Lista carrega, aba de documentos por máquina abre |
| Kanban | `/kanban-estagiarios` | Cards de estagiários carregam, contagem de "vencidas" no dashboard bate |
| Dashboard Estagiários | `/dashboard-estagiarios` | Sem erro de carregamento |

Para cada página: abrir o Console do DevTools e confirmar **zero erros JS** relacionados a `undefined`/`cannot read property` (sinal clássico de mudança de formato de campo não refletida no frontend).

- [ ] **Step 4: Parar o servidor**

---

### Task 6: Executar o merge

**Files:** nenhum arquivo de código alterado — só histórico git.

**Interfaces:**
- Consumes: Tasks 1–5 todas verdes.
- Produces: `main` local atualizada com os 36 commits da branch, via um commit de merge único (rastreável e reversível de uma vez, ao contrário de um fast-forward silencioso).

- [ ] **Step 1: Trocar para `main` e confirmar que está no mesmo commit checado na Task 1**

Run: `git checkout main && git log --oneline -1`
Expected: `14f5b39 docs: adicionar badges, sumario e secao de contribuidores ao README` (ou o commit mais recente, se Task 1/Step 2 encontrou avanço).

- [ ] **Step 2: Merge com commit explícito**

```bash
git merge --no-ff refactor/unified-model-phase1 -m "$(cat <<'EOF'
merge: integrar refactor/unified-model-phase1 (Phase 1 + Phase 2 Rodada 1)

Unificação de 19/20 Entities+DTOs (DocumentoPDF deliberadamente adiado,
Task 13g) e introdução de comportamento de dominio em 5 Entities
(PastaDocumento, DocumentoMaquina, Usuario, ItemAlmoxarifado, KanbanCard).
Verificado: suite automatizada completa, regressao manual via curl e
checagem visual no navegador (docs/superpowers/plans/2026-08-13-merge-refactor-unified-model-phase1.md).
EOF
)"
```
Expected: merge sem conflito (garantido pela Task 1, Step 2 — `main` não avançou desde a divergência), `Fast-forward` ou merge commit criado sem marcadores `<<<<<<<`.

---

### Task 7: Reverificar na própria `main`

**Files:** nenhum arquivo alterado.

**Interfaces:**
- Consumes: `main` pós-merge (Task 6).
- Produces: evidência que satisfaz literalmente o critério de pronto do item A1 do backlog: *"branch principal atualizada, build e suíte de testes passando na branch principal"*.

- [ ] **Step 1: Rodar a suíte na `main`**

Run: `cd pilot && mvn test`
Expected: `BUILD SUCCESS`, mesmo resultado da Task 2.

- [ ] **Step 2: Empacotar na `main`**

Run: `cd pilot && mvn package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Smoke test rápido** — repetir só o Step 3 da Task 4 (os 5 curls), agora apontando para o servidor subido a partir da `main`, para confirmar que o merge não alterou nada no caminho.

---

### Task 8: Push e plano de rollback

**Files:** nenhum arquivo alterado.

**Interfaces:**
- Consumes: Task 7 verde.
- Produces: `main` atualizada no remoto `integration`.

- [ ] **Step 1: Push — PEDIR CONFIRMAÇÃO EXPLÍCITA DO USUÁRIO ANTES**, por afetar o repositório compartilhado.

Run (só após confirmação): `git push integration main`

- [ ] **Step 2: Documentar o plano de rollback, para o caso de algo quebrar depois do push**

Se a Task 7 falhar **antes** do push: `git reset --hard 14f5b39` (ou o commit registrado na Task 1/Step 1) desfaz o merge local sem afetar ninguém.

Se o problema só aparecer **depois** do push: nunca usar `reset --hard` + force-push em `main` compartilhada. Reverter com:
```bash
git revert -m 1 <sha-do-commit-de-merge>
git push integration main
```
Isso cria um novo commit que desfaz o merge, preservando o histórico — mais seguro que reescrever a `main`.

---

## Resumo de critérios de pronto (rastreável ao item A1 do backlog)

- [ ] Task 1: zero desvio confirmado entre `main` e a branch
- [ ] Task 2: suíte completa verde na branch
- [ ] Task 3: build empacota sem erro
- [ ] Task 4: 5 pontos de regressão de maior risco reconfirmados via curl
- [ ] Task 5: 5 páginas do frontend renderizam sem erro de console
- [ ] Task 6: merge executado sem conflito
- [ ] Task 7: suíte completa verde **na `main`** + build empacota **na `main`**
- [ ] Task 8: push feito (com confirmação explícita) + rollback documentado
