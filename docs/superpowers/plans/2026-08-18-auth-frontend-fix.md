# Correção do Sistema Auth e Proteção de Rotas de Página — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the frontend `Auth` module reflect who is actually logged in — fixing the unreachable `/usuarios` page and the permanently-wrong "Usuário"/"Gestor" topbar — and back it with real server-side protection on administrative page routes.

**Architecture:** A new `@ControllerAdvice` injects the authenticated user's `nome` and `role` into the model of every page rendered by `PageController`. Each template's `<body>` carries those values as data attributes, so `core.js` reads them synchronously at load — no fetch, no `sessionStorage`, no intermediate state where the app doesn't know who the user is. The `Auth` module's permission tables are rekeyed from organizational cargo names to backend role names, and `SecurityConfig` gains rules for page routes so the JS guard stops being the only defense.

**Tech Stack:** Spring Boot 3.4.4 (`@ControllerAdvice` + `@ModelAttribute`, Spring Security `requestMatchers`), Thymeleaf (`th:attr`), vanilla JS (no framework), JUnit 5 + Mockito (via `spring-boot-starter-test`), Maven (`pilot/pom.xml`).

## Global Constraints

- Data reaches the frontend **rendered by the server**, never via `fetch` — the spec chose this specifically so no protected page can render before the guard decides (spec, "Decisão 1").
- `Auth` tables (`PERMS`, `PAGE_ROLES`, `NAV_ROLES`, `ROLE_COLORS`) are keyed by **role** (`ESTAGIARIO`/`TECNICO`/`ADMIN`), never by cargo (spec, "Decisão 2"). Mapping: `GESTOR`→`TECNICO`, `DIRETOR_CEM`→`ADMIN`, `ESTAGIARIO` keeps its name.
- Missing/unknown role degrades to **least privilege** (`ESTAGIARIO`), never escalates (spec, "Decisão 3" and "Tratamento de erro").
- `sessionStorage` and `Auth.set()` are **removed** from the module (spec, "Decisão 4").
- Server-side page rules must cover **all route aliases**, including `/lista-editais` and `/detalhes-edital` which currently escape the guard (spec, "Dois furos pré-existentes").
- Out of scope, do not do: frontend test infrastructure; backfilling the 2 users' NULL `cargo`; touching `login.html`/`qrcode-avaliacao.html`; consolidating duplicated topbar/sidebar markup into a Thymeleaf fragment (spec, "Fora de escopo").
- `mvn test` from `pilot/` must stay green (currently 23 tests).

---

### Task 1: `@ControllerAdvice` que injeta o usuário autenticado

**Files:**
- Create: `pilot/src/main/java/com/zeiss/pilot/config/UsuarioAtualAdvice.java`
- Test: `pilot/src/test/java/com/zeiss/pilot/config/UsuarioAtualAdviceTest.java` (create; `src/test/java/com/zeiss/pilot/config/` does not exist yet — create the directory)

**Interfaces:**
- Consumes: `UsuarioRepository.findByEmail(String)` returning `Optional<Usuario>` (existing, `pilot/src/main/java/com/zeiss/pilot/repository/UsuarioRepository.java`), and `Usuario.getNome()`/`Usuario.getRole()` (existing getters, both `String`).
- Produces: model attributes `usuarioNome` (String) and `usuarioRole` (String), consumed by Task 2's templates. Public method signature: `void adicionarUsuarioAtual(Principal principal, Model model)`.

This is the **first Mockito-based test in this codebase** — all 5 existing test classes (`UsuarioTest`, `KanbanCardTest`, etc.) are plain entity unit tests with no mocking. Mockito ships with `spring-boot-starter-test` (already in `pilot/pom.xml:50`), so no dependency needs to be added.

The advice is scoped with `assignableTypes = PageController.class`. Without that scope it would also run on every `@RestController` request, firing a `findByEmail` database query on every single API call for a value the JSON responses never use.

- [ ] **Step 1: Write the failing tests**

Create `pilot/src/test/java/com/zeiss/pilot/config/UsuarioAtualAdviceTest.java`:

```java
package com.zeiss.pilot.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.zeiss.pilot.entity.Usuario;
import com.zeiss.pilot.repository.UsuarioRepository;

class UsuarioAtualAdviceTest {

    @Test
    void usuarioAutenticado_injetaNomeERole() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setNome("Administrador");
        usuario.setRole("ADMIN");
        when(repo.findByEmail("admin@zeiss.com")).thenReturn(Optional.of(usuario));

        Model model = new ConcurrentModel();
        Principal principal = () -> "admin@zeiss.com";

        new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(principal, model);

        assertEquals("Administrador", model.getAttribute("usuarioNome"));
        assertEquals("ADMIN", model.getAttribute("usuarioRole"));
    }

    @Test
    void principalNulo_naoLancaENaoInjeta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Model model = new ConcurrentModel();

        assertDoesNotThrow(() -> new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(null, model));

        assertFalse(model.containsAttribute("usuarioNome"));
        assertFalse(model.containsAttribute("usuarioRole"));
    }

    @Test
    void usuarioAusenteNoBanco_naoLancaENaoInjeta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.findByEmail(anyString())).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        Principal principal = () -> "fantasma@zeiss.com";

        assertDoesNotThrow(() -> new UsuarioAtualAdvice(repo).adicionarUsuarioAtual(principal, model));

        assertFalse(model.containsAttribute("usuarioNome"));
        assertFalse(model.containsAttribute("usuarioRole"));
    }
}
```

`Principal` is a functional interface (single method `getName()`), which is why the lambda `() -> "admin@zeiss.com"` compiles. `ConcurrentModel` is Spring's plain in-memory `Model` implementation — no Spring context needed.

- [ ] **Step 2: Run tests to verify they fail**

Run (from `pilot/`): `mvn test -Dtest=UsuarioAtualAdviceTest`
Expected: COMPILATION FAILURE — `UsuarioAtualAdvice` does not exist yet.

- [ ] **Step 3: Write the implementation**

Create `pilot/src/main/java/com/zeiss/pilot/config/UsuarioAtualAdvice.java`:

```java
package com.zeiss.pilot.config;

import java.security.Principal;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zeiss.pilot.controller.PageController;
import com.zeiss.pilot.repository.UsuarioRepository;

// Injeta nome e role do usuario autenticado no Model de toda pagina renderizada
// pelo PageController, para que o Auth do core.js leia esses dados direto do HTML
// (sem fetch assincrono, sem estado intermediario onde o app nao sabe quem esta logado).
//
// Escopado a PageController de proposito: sem assignableTypes, este advice rodaria
// tambem em toda requisicao de @RestController, disparando um findByEmail por
// chamada de API para um valor que as respostas JSON nunca usam.
@ControllerAdvice(assignableTypes = PageController.class)
public class UsuarioAtualAdvice {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAtualAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void adicionarUsuarioAtual(Principal principal, Model model) {
        // Principal nulo = usuario anonimo (ex.: /avaliacao, que e' permitAll e
        // carrega core.js). Usuario ausente no banco = sessao viva de usuario
        // deletado. Nos dois casos nao injeta nada e o Auth cai no menor
        // privilegio (ESTAGIARIO) — nunca escala privilegio por falta de dado.
        if (principal == null) {
            return;
        }
        usuarioRepository.findByEmail(principal.getName()).ifPresent(usuario -> {
            model.addAttribute("usuarioNome", usuario.getNome());
            model.addAttribute("usuarioRole", usuario.getRole());
        });
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run (from `pilot/`): `mvn test -Dtest=UsuarioAtualAdviceTest`
Expected: 3 tests PASS.

- [ ] **Step 5: Run the full suite for regressions**

Run (from `pilot/`): `mvn test`
Expected: BUILD SUCCESS, 26 tests (23 existing + 3 new), 0 failures.

- [ ] **Step 6: Commit**

```bash
git add pilot/src/main/java/com/zeiss/pilot/config/UsuarioAtualAdvice.java pilot/src/test/java/com/zeiss/pilot/config/UsuarioAtualAdviceTest.java
git commit -m "feat(auth): inject authenticated user's nome and role into page models

Scoped to PageController so REST requests don't pay for a findByEmail
they never use. Anonymous requests and sessions whose user no longer
exists inject nothing, letting the frontend fall back to least privilege."
```

---

### Task 2: Atributos do usuário na tag `<body>` dos templates

**Files:**
- Modify (20 files, identical one-line change in each): `pilot/src/main/resources/templates/` — `404.html`, `almoxarifado.html`, `amostras.html`, `avaliacao.html`, `dashboard-avaliacao.html`, `dashboard-estagiarios.html`, `dashboardEventos.html`, `dashboardServicos.html`, `detalhes-edital.html`, `documentos.html`, `index.html`, `kanban-estagiarios.html`, `lista-editais.html`, `lista-eventos.html`, `maquinas.html`, `projetos.html`, `servicos.html`, `usuarios.html`, `verificacao-ambiental.html`, `visitasTecnicas.html`

**Interfaces:**
- Consumes: model attributes `usuarioNome` and `usuarioRole` produced by Task 1's `UsuarioAtualAdvice`.
- Produces: DOM attributes `data-user-nome` and `data-user-role` on `<body>`, read by Task 3's `Auth.role()`/`Auth.user()` as `document.body.dataset.userRole` / `document.body.dataset.userNome` (the browser's dataset API converts `data-user-role` to `userRole`).

Do **not** modify `login.html` or `qrcode-avaliacao.html` — they do not load `core.js`, so the attributes would serve no purpose (spec, "Fora de escopo").

All 20 files currently have the exact same body tag, with no attributes: `<body>`. Two of the 20 are special but still get the same edit:
- `avaliacao.html` is `permitAll` and reachable anonymously by QR code — Task 1 handles the null `Principal`, so Thymeleaf simply renders no attributes there.
- `404.html` is an error page not served by `PageController`, so the advice never runs for it and the attributes render empty. That is harmless: `Auth` falls back to `ESTAGIARIO`.

- [ ] **Step 1: Replace the body tag in all 20 templates**

In each of the 20 files listed above, replace the line:

```html
<body>
```

with:

```html
<body th:attr="data-user-nome=${usuarioNome},data-user-role=${usuarioRole}">
```

When a model attribute is absent, Thymeleaf's `th:attr` omits that attribute entirely rather than writing `data-user-role="null"` — which is exactly the behavior the least-privilege fallback depends on.

- [ ] **Step 2: Verify all 20 were changed and nothing else was**

Run (from repo root):

```bash
grep -c 'th:attr="data-user-nome' pilot/src/main/resources/templates/*.html | grep -v ':0$'
```

Expected: exactly 20 lines listed, each ending in `:1`. Then confirm the two excluded files are untouched:

```bash
grep -n '<body' pilot/src/main/resources/templates/login.html pilot/src/main/resources/templates/qrcode-avaliacao.html
```

Expected: both still show a plain `<body>`.

- [ ] **Step 3: Confirm the app still builds**

Run (from `pilot/`): `mvn test`
Expected: BUILD SUCCESS, 26 tests, 0 failures. (No test reads templates; this is a regression check that nothing was broken syntactically.)

- [ ] **Step 4: Commit**

```bash
git add pilot/src/main/resources/templates/
git commit -m "feat(auth): carry authenticated user's nome and role on the body tag

Renders the data the Auth module needs directly into the page, so it is
available synchronously at load instead of arriving later via fetch.
login.html and qrcode-avaliacao.html are excluded — they don't load core.js."
```

---

### Task 3: Reescrita do módulo `Auth` no `core.js`

**Files:**
- Modify: `pilot/src/main/resources/static/js/core.js:303-422` (the entire `Auth` IIFE)

**Interfaces:**
- Consumes: `data-user-nome` / `data-user-role` on `<body>`, produced by Task 2.
- Produces: `Auth.role()` → String (one of `ESTAGIARIO`/`TECNICO`/`ADMIN`), `Auth.user()` → `{nome: String}` or `null`, `Auth.can(String)` → boolean, `Auth.init()` → void, plus `Auth.ROLES` and `Auth.PERMS` objects. `Auth.set()` is **removed** — nothing calls it (verified: the only `Auth.` reference anywhere in `static/js` is `Auth.init()` at `core.js:454`).

`Auth.can()`/`Auth.PERMS` are consumed only inside `core.js` itself, so this rewrite cannot break other JS files.

Note the distinction between two similarly-named attributes: `applyBodyRole()` writes `document.body.dataset.role` (i.e. `data-role`, used for CSS targeting), which is **different** from the `data-user-role` this task reads. Both coexist; do not merge them.

- [ ] **Step 1: Replace the Auth module**

Replace `core.js` lines 303-422 (from `/* ── Auth / Role System ── */` through the closing `})();` of the `Auth` IIFE) with:

```js
/* ── Auth / Role System ── */
// Fonte da verdade: atributos data-user-* renderizados pelo servidor na tag <body>
// (ver UsuarioAtualAdvice.java). Nao usa sessionStorage de proposito: dado de sessao
// sobreviveria a troca de usuario na mesma aba, fazendo a role do primeiro valer
// para o segundo.
const Auth = (() => {

  const ROLES = {
    ESTAGIARIO: 'Estagiário',
    TECNICO:    'Técnico',
    ADMIN:      'Administrador',
  };

  const PERMS = {
    ESTAGIARIO: { delete: false, edit: false, viewFinancial: false, viewEditais: false, viewDocumentos: false, viewUsuarios: false, create: true  },
    TECNICO:    { delete: true,  edit: true,  viewFinancial: true,  viewEditais: true,  viewDocumentos: true,  viewUsuarios: false, create: true  },
    ADMIN:      { delete: true,  edit: true,  viewFinancial: true,  viewEditais: true,  viewDocumentos: true,  viewUsuarios: true,  create: true  },
  };

  // Comparado com window.location.pathname, que pode ser qualquer apelido de rota —
  // por isso lista /lista-editais e /detalhes-edital alem das rotas canonicas.
  const PAGE_ROLES = {
    '/editais/lista':   ['TECNICO', 'ADMIN'],
    '/lista-editais':   ['TECNICO', 'ADMIN'],
    '/detalhes-edital': ['TECNICO', 'ADMIN'],
    '/documentos':      ['TECNICO', 'ADMIN'],
    '/usuarios':        ['ADMIN'],
  };

  // Comparado com o atributo href dos itens da sidebar, que usa so' rotas canonicas.
  const NAV_ROLES = {
    '/editais/lista': ['TECNICO', 'ADMIN'],
    '/documentos':    ['TECNICO', 'ADMIN'],
    '/usuarios':      ['ADMIN'],
  };

  const ROLE_COLORS = {
    ESTAGIARIO: '#6b7280',
    TECNICO:    '#2563eb',
    ADMIN:      '#7c3aed',
  };

  // Sem dado renderizado (pagina anonima, usuario deletado, pagina de erro),
  // assume o MENOR privilegio — nunca escala por falta de informacao.
  function role() { return document.body?.dataset.userRole || 'ESTAGIARIO'; }

  function user() {
    const nome = document.body?.dataset.userNome;
    return nome ? { nome } : null;
  }

  function can(p) { return !!((PERMS[role()] || {})[p]); }

  function guardPage() {
    const path = window.location.pathname;
    for (const [page, allowed] of Object.entries(PAGE_ROLES)) {
      if (path === page || path.startsWith(page + '/') || path.startsWith(page + '?')) {
        if (!allowed.includes(role())) { window.location.replace('/'); return false; }
      }
    }
    return true;
  }

  function applyNav() {
    const r = role();
    document.querySelectorAll('.sidebar__nav-item[href]').forEach(a => {
      const href = a.getAttribute('href');
      if (NAV_ROLES[href] && !NAV_ROLES[href].includes(r)) {
        a.style.display = 'none';
      }
    });
    document.querySelectorAll('.sidebar__section-label').forEach(label => {
      let sib = label.nextElementSibling;
      let allHidden = true;
      while (sib && !sib.classList.contains('sidebar__section-label')) {
        if (sib.classList.contains('sidebar__nav-item') && sib.style.display !== 'none') {
          allHidden = false; break;
        }
        sib = sib.nextElementSibling;
      }
      if (allHidden) label.style.display = 'none';
    });
  }

  // data-role (para CSS) e' distinto do data-user-role lido em role().
  function applyBodyRole() {
    document.body.dataset.role = role();
  }

  function showRoleBadge() {
    const r = role();
    const u = user();

    // Topbar name
    const nameEl = document.getElementById('topbarName');
    if (nameEl && u?.nome) nameEl.textContent = u.nome;

    // Avatar initials
    const initials = u?.nome
      ? u.nome.split(/\s+/).slice(0,2).map(w => w[0]?.toUpperCase() || '').join('')
      : 'U';
    document.querySelectorAll('.topbar__user-avatar, #dropdownAvatar').forEach(el => {
      el.textContent = initials;
    });

    // Dropdown user info
    const dropName = document.getElementById('dropdownName');
    const dropRole = document.getElementById('dropdownRole');
    if (dropName && u?.nome) dropName.textContent = u.nome;
    if (dropRole) {
      const color = ROLE_COLORS[r] || '#6b7280';
      dropRole.textContent = ROLES[r] || r;
      dropRole.style.cssText = `font-size:10px;font-weight:600;color:${color};margin-top:2px`;
    }

    // Esconde "Gerenciar Usuários" se nao for ADMIN
    const manageLink = document.querySelector('.topbar__dropdown a[href="/usuarios"]');
    if (manageLink && !can('viewUsuarios')) manageLink.style.display = 'none';
  }

  function init() {
    applyBodyRole();
    if (!guardPage()) return;
    applyNav();
    showRoleBadge();
  }

  return { role, user, can, init, ROLES, PERMS };
})();
```

- [ ] **Step 2: Verify the removals actually happened**

Run (from repo root):

```bash
grep -n "sessionStorage\|zp-role\|zp-user\|GESTOR\|DIRETOR_CEM" pilot/src/main/resources/static/js/core.js
```

Expected: **no output** for the `Auth` module's former storage keys and cargo names. (If `sessionStorage` appears on a line outside the `Auth` module — e.g. in `Theme` or `Sidebar`, which use `localStorage` — re-read the hit and leave unrelated code alone.)

```bash
grep -n "function set\|Auth.set" pilot/src/main/resources/static/js/core.js
```

Expected: no output.

- [ ] **Step 3: Confirm the app still builds**

Run (from `pilot/`): `mvn test`
Expected: BUILD SUCCESS, 26 tests, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add pilot/src/main/resources/static/js/core.js
git commit -m "fix(auth): read the real logged-in user instead of defaulting to GESTOR

Auth.set() was never called anywhere, so role() always returned the
hardcoded 'GESTOR' fallback and user() always returned null — making
/usuarios unreachable even for an ADMIN and pinning the topbar to
\"Usuário\"/\"Gestor\" for everyone.

Auth now reads the server-rendered body attributes, drops sessionStorage
(which would leak one user's role to the next in the same tab), rekeys
its tables from cargo names to backend roles, falls back to least
privilege, and guards the /lista-editais and /detalhes-edital route
aliases that previously slipped past it."
```

---

### Task 4: Regras server-side para as rotas de página

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java:24-39` (the `authorizeHttpRequests` block)

**Interfaces:**
- Consumes: role values `ADMIN` / `TECNICO` as Spring Security authorities. `CustomUserDetailsService` (unchanged) already builds them generically as `"ROLE_" + usuario.getRole()`, so `hasRole`/`hasAnyRole` match without any change there.
- Produces: nothing consumed by later tasks — Task 5 verifies the effect over HTTP.

Until now the only protection on these pages was the JS guard, which is bypassable by disabling JavaScript or requesting the HTML with curl. The JS guard stays in place as a UX layer (don't show a link that leads to a 403); this task makes it stop being the only defense.

There is no `@WebMvcTest`/MockMvc infrastructure in this repo (confirmed: `src/test/java` contains only `PilotApplicationTests` and 5 entity test classes), so this task is verified by curl in Task 5 rather than by a JUnit test. Do not introduce that infrastructure here — it is out of scope (spec, "Fora de escopo").

- [ ] **Step 1: Add the page-route rules**

In `SecurityConfig.java`, the current `authorizeHttpRequests` block reads:

```java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/css/**", "/img/**", "/js/**",
                    "/avaliacao", "/qrcode-avaliacao"
                ).permitAll()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/avaliacoes").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/eventos").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.PUT, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.PATCH, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.DELETE, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
```

Replace it with (two new lines added immediately before `.requestMatchers("/api/**")`):

```java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/css/**", "/img/**", "/js/**",
                    "/avaliacao", "/qrcode-avaliacao"
                ).permitAll()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/avaliacoes").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/eventos").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.PUT, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.PATCH, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.DELETE, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
                .requestMatchers("/usuarios").hasRole("ADMIN")
                .requestMatchers("/documentos", "/lista-editais", "/editais/**", "/detalhes-edital").hasAnyRole("TECNICO", "ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
```

The two new matchers list **every alias** each page answers on: `/lista-editais` and `/editais/**` are two routes to the same editais list (`PageController:72`), and `/detalhes-edital` (`PageController:77`) is the real details route — the JS guard had been looking for a non-existent `/editais/detalhes`. Order matters (first match wins), but these paths don't overlap with any `/api/**` rule above them, so placing them just before the `/api/**` fallback is safe and keeps page rules grouped.

- [ ] **Step 2: Run the full suite for regressions**

Run (from `pilot/`): `mvn test`
Expected: BUILD SUCCESS, 26 tests, 0 failures.

- [ ] **Step 3: Commit**

```bash
git add pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java
git commit -m "feat(security): protect administrative page routes server-side

/usuarios now requires ADMIN; /documentos and the editais pages require
TECNICO or ADMIN. Until now these pages were served to any authenticated
user and guarded only by JavaScript, which a client can simply not run.

Covers every alias each page answers on, including /lista-editais and
/detalhes-edital — two routes the JS guard never matched, the latter
because it looked for an /editais/detalhes route that does not exist."
```

---

### Task 5: Verificação manual (curl + browser)

**Files:** none (no code changes — this task exercises the running application)

**Interfaces:**
- Consumes: the running app (`pilot/`, port 8090 per `application.properties:8`) against the local dev Postgres, plus the ADMIN login `admin@zeiss.com` / `SENHA_DE_TESTE`.
- Produces: nothing for later tasks — this is the acceptance gate for the whole plan.

**CSRF warning, learned the hard way on the previous feature:** Spring Security's default CSRF protection is fully active with no exemptions, so `POST` requests (including `/login` itself) fail with a redirect loop unless a CSRF token is supplied. For the login POST, first `GET /login` with a cookie jar and extract the hidden `_csrf` input; for JSON API POSTs, extract the token from the `<meta name="_csrf">` tag on an authenticated page and send it as an `X-CSRF-TOKEN` header. Budget for this — the naive curl recipe does not work here.

- [ ] **Step 1: Start the app**

Run (from `pilot/`) in a background process: `mvn spring-boot:run`
Wait for `Started PilotApplication` in the output before continuing. Run the remaining steps from a second shell.

- [ ] **Step 2: Log in as ADMIN and create the two test users**

Log in as `admin@zeiss.com` / `SENHA_DE_TESTE` (CSRF-aware, per the warning above) into `cookies-admin.txt`. Before creating, check whether a previous run left them behind:

```bash
curl -b cookies-admin.txt "http://localhost:8090/api/usuarios?role=TECNICO" -s | grep -o '"email":"[^"]*"'
curl -b cookies-admin.txt "http://localhost:8090/api/usuarios?role=ESTAGIARIO" -s | grep -o '"email":"[^"]*"'
```

If `tecnico.auth@zeiss.com` / `estagiario.auth@zeiss.com` already exist, reuse them and skip creating. Otherwise create both (send the CSRF token as an `X-CSRF-TOKEN` header):

```bash
# cargo GESTOR derives role TECNICO; cargo ESTAGIARIO derives role ESTAGIARIO
curl -b cookies-admin.txt -X POST http://localhost:8090/api/usuarios \
  -H "Content-Type: application/json" -H "X-CSRF-TOKEN: <token>" \
  -d '{"nome":"Tecnico Auth","email":"tecnico.auth@zeiss.com","senha":"SENHA_DE_TESTE","cargo":"GESTOR"}' \
  -s | grep -o '"role":"[^"]*"'

curl -b cookies-admin.txt -X POST http://localhost:8090/api/usuarios \
  -H "Content-Type: application/json" -H "X-CSRF-TOKEN: <token>" \
  -d '{"nome":"Estagiario Auth","email":"estagiario.auth@zeiss.com","senha":"SENHA_DE_TESTE","cargo":"ESTAGIARIO"}' \
  -s | grep -o '"role":"[^"]*"'
```

Expected: `"role":"TECNICO"` and `"role":"ESTAGIARIO"` respectively. If either differs, stop — something regressed in `derivarRoleDoCargo()`.

- [ ] **Step 3: Log in as each test user**

Log in (CSRF-aware) as `tecnico.auth@zeiss.com` into `cookies-tecnico.txt` and `estagiario.auth@zeiss.com` into `cookies-estagiario.txt`, both with password `SENHA_DE_TESTE`. Confirm each lands on `/index` with `200`.

- [ ] **Step 4: Verify the new server-side page rules**

```bash
for who in admin tecnico estagiario; do
  for page in /usuarios /documentos /lista-editais /editais/lista /detalhes-edital; do
    printf "%s %s: " "$who" "$page"
    curl -b cookies-$who.txt "http://localhost:8090$page" -s -o /dev/null -w "%{http_code}\n"
  done
done
```

Expected:

| Page | admin | tecnico | estagiario |
|---|---|---|---|
| `/usuarios` | 200 | 403 | 403 |
| `/documentos` | 200 | 200 | 403 |
| `/lista-editais` | 200 | 200 | 403 |
| `/editais/lista` | 200 | 200 | 403 |
| `/detalhes-edital` | 200 | 200 | 403 |

The last three rows are the previously-unguarded aliases — a `403` there for `estagiario` is the specific regression this plan closes.

- [ ] **Step 5: Verify the body attributes are actually rendered**

```bash
curl -b cookies-admin.txt http://localhost:8090/index -s | grep -o '<body[^>]*>'
curl -b cookies-estagiario.txt http://localhost:8090/index -s | grep -o '<body[^>]*>'
curl http://localhost:8090/avaliacao -s | grep -o '<body[^>]*>'
```

Expected: the admin request shows `data-user-nome` and `data-user-role="ADMIN"`; the estagiario request shows `data-user-role="ESTAGIARIO"` with that user's name; the anonymous `/avaliacao` request renders a `<body>` with **no** `data-user-*` attributes and returns normally (not a 500) — that is the null-`Principal` path working.

- [ ] **Step 6: Verify in the browser**

Log in as `admin@zeiss.com` / `SENHA_DE_TESTE` at `http://localhost:8090/login` and confirm all four:
1. The topbar shows the user's real name — **not** the placeholder "Usuário".
2. The dropdown shows **"Administrador"** — not "Gestor".
3. The "Usuários" item is visible in the sidebar, and `/usuarios` **opens** instead of bouncing back to `/`.
4. The avatar shows the user's initials rather than "U".

- [ ] **Step 7: Clean up**

Delete the two test users as ADMIN (`DELETE /api/usuarios/{id}`, CSRF-aware; expect `204` each), stop the app, and remove the cookie jars:

```bash
rm -f cookies-admin.txt cookies-tecnico.txt cookies-estagiario.txt
```

Confirm with `git status` that the working tree has no unintended changes.

- [ ] **Step 8: Report the result**

No commit for this task (no files change). Report the outcome — especially the Step 4 status table and the Step 6 browser observations — rather than silently proceeding.

---

## Self-Review Notes

- **Spec coverage:** "Decisão 1" (Thymeleaf/ControllerAdvice) → Tasks 1-2. "Decisão 2" (rekey to roles) → Task 3. "Decisão 3" (least-privilege fallback) → Task 3 `role()`. "Decisão 4" (drop sessionStorage/`set()`) → Task 3, verified in its Step 2. "Arquitetura"/`SecurityConfig` → Task 4. "Tratamento de erro" (3 cases) → Task 1's three tests, plus Task 5 Step 5's anonymous check. "Testes" (unit + curl + browser) → Tasks 1 and 5. Route-alias fix → Tasks 3 and 4, verified in Task 5 Step 4.
- **Placeholder scan:** the only bracketed placeholder is `<token>` in Task 5 Step 2, which is a runtime-fetched CSRF value that cannot be known in advance; how to obtain it is spelled out in the task's CSRF warning.
- **Type consistency:** the model attribute names `usuarioNome`/`usuarioRole` (Task 1) match the `th:attr` expressions (Task 2), which produce `data-user-nome`/`data-user-role`, which match `dataset.userNome`/`dataset.userRole` (Task 3). The role literals `ESTAGIARIO`/`TECNICO`/`ADMIN` are identical across Task 3's tables, Task 4's `hasAnyRole` calls, and Task 5's expected results.