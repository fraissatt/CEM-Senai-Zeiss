# Correção do Sistema Auth (frontend) e Proteção de Rotas de Página — Design

**Origem:** trabalho de continuação, solicitado pelo usuário depois de esbarrar no bug ao usar o app logo após a entrega do B2 (papel `TECNICO`). Era explicitamente "fora de escopo" do B2 — ver `2026-08-17-papel-tecnico-design.md`, seção "Fora de escopo".

## Contexto: o bug

`Auth.set(role, user)` existe em `core.js:343-346` mas **nunca é chamado em lugar nenhum do projeto** (confirmado por grep em todo `static/js`). Como consequência, as duas funções de leitura sempre caem no caminho degradado:

```js
function role() { return sessionStorage.getItem(KEY) || 'GESTOR'; }  // core.js:339
function user() { try { return JSON.parse(sessionStorage.getItem(UKEY)); } catch { return null; } }  // core.js:340
```

`role()` retorna **sempre** `'GESTOR'` e `user()` retorna **sempre** `null`, independente de quem está autenticado.

### Sintomas observados pelo usuário no app rodando

1. **A tela `/usuarios` é inalcançável.** `PAGE_ROLES['/usuarios'] = ['DIRETOR_CEM']` (`core.js:324`) e `guardPage()` (`core.js:348-356`) redireciona pra `/` quando `role()` não está na lista. Como `role()` é sempre `'GESTOR'`, ninguém nunca acessa — nem um `ADMIN` real. O usuário relatou que "não existe tela /usuarios"; ela existe (rota, template, JS completos), mas expulsa todo mundo antes de renderizar.
2. **Topbar mostra "Usuário" e "Gestor" pra qualquer um.** `showRoleBadge()` (`core.js:383-412`) só substitui o nome se `user()?.nome` existir — como é sempre `null`, fica o placeholder estático do HTML. E a role exibida é `ROLES[role()]` = `ROLES['GESTOR']` = `"Gestor"`, mesmo logado como `ADMIN`.
3. **Link "Gerenciar Usuários" com visibilidade errada** (`core.js:410-411`), pela mesma causa.

### Dois furos pré-existentes no guard, por desalinhamento de rota

`PageController` expõe páginas com **rotas duplicadas** (apelidos), mas `PAGE_ROLES` só conhece uma variante de cada:

| Página | Rotas reais (`PageController`) | Rota no `PAGE_ROLES` | Situação |
|---|---|---|---|
| Lista de editais | `/lista-editais` **e** `/editais/lista` (`:72`) | só `/editais/lista` | `/lista-editais` escapa do guard |
| Detalhes do edital | `/detalhes-edital` (`:77`) | `/editais/detalhes` | rota do guard **não existe** — página nunca guardada |
| Documentos | `/documentos` (`:35`) | `/documentos` | ok |
| Usuários | `/usuarios` (`:30`) | `/usuarios` | ok |

Ou seja: mesmo com o `Auth` funcionando, o guard atual seria contornável por URL alternativa.

### Nenhuma proteção server-side nas rotas de página

`SecurityConfig` protege `/api/**`, mas nenhuma rota de página (`/usuarios`, `/documentos`, `/lista-editais`, `/detalhes-edital`). Elas caem no `.anyRequest().authenticated()` — servidas pra **qualquer autenticado**. A proteção é exclusivamente client-side, contornável desabilitando JS ou pedindo o HTML por curl. O conteúdo dinâmico não viria (as APIs continuam protegidas), mas a estrutura da página, sim.

## Estado do ambiente (verificado, não presumido)

- `core.js` é arquivo único, incluído em **20 dos 22** templates (exceções: `login.html`, `qrcode-avaliacao.html`). Correção no JS se propaga sozinha.
- **Não existe fragmento Thymeleaf reutilizável** (`th:fragment`/`th:replace` não aparecem em nenhum template) — a marcação de topbar/sidebar é duplicada em 19 arquivos.
- As 22 tags `<body>` são **idênticas e sem atributos** (`<body>`), então a edição é a mesma linha em cada arquivo.
- `avaliacao.html` é **pública** (`permitAll` no `SecurityConfig`) **e** carrega `core.js` — logo, acesso anônimo com o `Auth` ativo é um caso real, não hipotético.
- `Auth.can()`/`Auth.PERMS` são consumidos **apenas dentro do próprio `core.js`** — nenhum outro JS do projeto os consulta. O raio de impacto do bug é: guard de página, visibilidade de item de menu, nome/role no topbar, link "Gerenciar Usuários".
- Já existe o padrão de resolver o usuário atual pelo `Principal` no backend (`UsuarioController.validarSenhaAdmin`, `:69-73`).
- Os 2 usuários reais do banco têm `cargo` **NULL** e `role = 'ADMIN'` — foram criados direto no banco, antes de todo esse fluxo existir (confirmado pelo usuário).

## Decisão 1: dados vêm renderizados pelo servidor, não por fetch

**Escolhido: Thymeleaf via `@ControllerAdvice` global.** Descartado: endpoint novo `GET /api/usuarios/me` + `fetch` no `core.js`.

Motivo principal — **não existe estado intermediário errado**. Com fetch, uma página protegida renderiza, aparece na tela, e só depois o JS descobre que o usuário não deveria estar ali e redireciona: um estagiário veria a tela de gerenciamento de usuários piscar antes de ser expulso. Com dado renderizado, o guard decide antes de qualquer pixel aparecer.

Motivo secundário: o bug atual nasceu justamente de um fallback silencioso (`|| 'GESTOR'`) preenchendo a ausência de dado. Fetch adiciona *mais* um momento em que o app não sabe quem é o usuário (requisição lenta ou falha). Dado no HTML elimina esse estado nas páginas autenticadas.

Um `@ControllerAdvice` evita tocar nos ~25 métodos do `PageController`. O contra é editar a `<body>` de ~20 templates, mas é uma linha idêntica em cada, mecânica e trivial de revisar.

## Decisão 2: tabelas do Auth migram de cargo para role

As tabelas (`PERMS`, `PAGE_ROLES`, `NAV_ROLES`, `ROLE_COLORS`) são hoje chaveadas por **cargo** (`ESTAGIARIO`/`GESTOR`/`DIRETOR_CEM`) — vocabulário anterior ao B2, que desconhece as roles `TECNICO`/`ADMIN`.

**Passam a ser chaveadas por role** (`ESTAGIARIO`/`TECNICO`/`ADMIN`), alinhando frontend e backend.

O argumento decisivo é prático: os 2 usuários reais têm `cargo` NULL. Chaveando por cargo, eles não casariam com nenhuma entrada e o sistema de permissões ficaria quebrado pra todos os usuários existentes. Chaveando por role, ambos batem em `ADMIN`.

Mapeamento das tabelas: `GESTOR` → `TECNICO`, `DIRETOR_CEM` → `ADMIN`, `ESTAGIARIO` mantém o nome (agora significando a role, não o cargo).

O `cargo` NULL desses 2 registros continua sendo dado incompleto no banco, mas deixa de ter qualquer efeito sobre permissões. **Backfill desses registros está fora do escopo deste trabalho.**

## Decisão 3: fallback passa a ser o menor privilégio

`role()` deixa de cair em `'GESTOR'` (intermediário) e passa a cair em `'ESTAGIARIO'` (menor privilégio). Se o dado faltar por qualquer motivo, o app nega em vez de conceder.

## Decisão 4: `Auth` lê do DOM, `sessionStorage` sai

Com o dado renderizado no `<body>`, `sessionStorage` deixa de ter função e é removido do módulo. Isso elimina um bug latente: dois usuários logando na mesma aba em sequência fariam o `sessionStorage` do primeiro valer pro segundo. Dado renderizado pelo servidor não fica velho.

`Auth.set()` — a função que ninguém chama — deixa de existir.

## Arquitetura

**Backend (novo):** um `@ControllerAdvice` resolve o usuário autenticado pelo `Principal` e injeta `usuarioNome` e `usuarioRole` no `Model` de toda página renderizada.

**Templates (~20 arquivos):** `<body>` passa a carregar os dados:

```html
<body th:attr="data-user-nome=${usuarioNome},data-user-role=${usuarioRole}">
```

**`core.js`:** `Auth.role()`/`Auth.user()` leem `document.body.dataset`; tabelas rechaveadas por role; apelidos de rota faltantes adicionados em `PAGE_ROLES`; `set()` e `sessionStorage` removidos.

`NAV_ROLES` **não** precisa dos apelidos: ele é comparado com o atributo `href` dos itens do menu (`core.js:361-362`), e a sidebar usa só as rotas canônicas (`/editais/lista`, `/documentos`, `/usuarios` — verificado em `index.html`). Já `PAGE_ROLES` é comparado com `window.location.pathname` (`core.js:349-352`), que pode ser qualquer apelido — por isso só ele precisa da correção.

**`SecurityConfig`:** regras novas pras rotas de página, **cobrindo todos os apelidos**:

- `/usuarios` → `hasRole("ADMIN")`
- `/documentos`, `/lista-editais`, `/editais/**`, `/detalhes-edital` → `hasAnyRole("TECNICO", "ADMIN")`

Posicionadas antes do `.anyRequest().authenticated()`, seguindo a ordem de precedência já usada no B2 (primeira regra que casa vence).

O guard do JS permanece — como camada de UX (não exibir link que levaria a 403), não como defesa única.

## Tratamento de erro

Três casos que o `ControllerAdvice` trata explicitamente, todos degradando pro **menor privilégio**, nunca escalando:

1. **`Principal` nulo (anônimo).** `/avaliacao` é `permitAll` e carrega `core.js` — acessada por QR code sem login. Não pode estourar exceção; não injeta atributo, e o `Auth` cai em `ESTAGIARIO`.
2. **Autenticado mas ausente no banco** (usuário deletado com sessão viva). Mesmo tratamento: não injeta, cai no fallback.
3. **`role` nula ou desconhecida.** `PERMS[role]` seria `undefined`; `can()` já trata (`(PERMS[role()] || {})[p]` → `false`, `core.js:341`). Comportamento seguro já existe, mas ganha teste — é a diferença entre "nega tudo" e "quebra a página".

## Testes

1. **Unitários (JUnit)** no `ControllerAdvice`, cobrindo os três casos acima: usuário autenticado retorna nome e role corretos; `Principal` nulo não estoura; usuário inexistente no banco não estoura. Seguem o padrão dos testes já existentes no projeto.
2. **Manual via curl** (mesmo formato do B2, que pegou coisa que teste não pegaria): por HTTP status, `GET /usuarios` → 200 pra `ADMIN`, 403 pra `TECNICO` e `ESTAGIARIO`; `/lista-editais` e `/detalhes-edital` (os apelidos que hoje escapam) → 403 pra `ESTAGIARIO`, 200 pra `TECNICO`/`ADMIN`.
3. **Verificação visual no browser:** logado como `ADMIN`, o topbar exibe o nome real do usuário e "Administrador" — não "Usuário"/"Gestor".

**Nota de CSRF:** verificação por curl neste projeto precisa de token CSRF (proteção padrão do Spring Security ativa, sem isenções) — ver a mesma nota no design do B2.

## Fora de escopo

- **Testes automatizados de frontend** — o projeto não tem essa infraestrutura; montá-la é um trabalho à parte.
- **Backfill do `cargo` NULL** dos 2 usuários existentes (ver Decisão 2).
- **`login.html` e `qrcode-avaliacao.html`** — não carregam `core.js`, não precisam dos atributos no `<body>`.
- **Consolidar a marcação duplicada de topbar/sidebar em fragmento Thymeleaf** — melhoria legítima e tentadora enquanto se edita 20 arquivos, mas é refatoração independente deste bug; misturar as duas coisas tornaria a revisão desta correção muito mais difícil.
