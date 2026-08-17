# Papel TÉCNICO no Controle de Acesso — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce the `TECNICO` role (derived from the `GESTOR` cargo) and enforce it as a real authorization boundary on write operations for `/api/maquinas/**` and `/api/servicos/**`, satisfying backlog item B2.

**Architecture:** Two independent, additive changes to existing code — no new files, no new endpoints. (1) `Usuario.derivarRoleDoCargo()` gets a third branch so `GESTOR` maps to `TECNICO` instead of `ADMIN`, and `ESTAGIARIO`/empty maps to `ESTAGIARIO` instead of `CLIENTE`. (2) `SecurityConfig.java` gets four new `requestMatchers` entries restricting POST/PUT/PATCH/DELETE on the two module paths to `ADMIN` or `TECNICO`, positioned before the generic `/api/**` fallback so they take precedence.

**Tech Stack:** Spring Boot (Spring Security `requestMatchers` + role-based `hasAnyRole`), JUnit 5, Maven (`pilot/pom.xml`), manual `curl` verification against a running local instance (Postgres-backed, no in-memory security test infra exists in this repo).

## Global Constraints

- No new cargo is introduced — only the 3 that already exist: `ESTAGIARIO`, `GESTOR`, `DIRETOR_CEM` (per spec, "Mapeamento cargo → role").
- `GET` on `/api/maquinas/**` and `/api/servicos/**` stays open to any authenticated user — only write verbs (POST/PUT/PATCH/DELETE) get the new restriction (per spec, "Nova regra de autorização").
- `CustomUserDetailsService` needs no change — it already builds the authority generically as `"ROLE_" + usuario.getRole()` (per spec, confirmed at `pilot/src/main/java/com/zeiss/pilot/security/CustomUserDetailsService.java:41`).
- The frontend `Auth` permissions system in `core.js` is explicitly out of scope for B2 — do not touch it (per spec, "Fora de escopo").
- No other endpoint besides `/api/maquinas/**` and `/api/servicos/**` is touched (per spec, "Fora de escopo").
- All existing tests must keep passing; `mvn test` is the verification command, run from `pilot/`.

---

### Task 1: `derivarRoleDoCargo()` — three-way role mapping

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/entity/Usuario.java:32` (default field value), `:66-72` (method body)
- Modify: `pilot/src/test/java/com/zeiss/pilot/entity/UsuarioTest.java:11-39` (update 3 existing tests, add 1 new test)

**Interfaces:**
- Consumes: nothing new — `Usuario.cargo` (String, nullable) and `Usuario.role` (String) already exist as instance fields with getters/setters.
- Produces: `Usuario.derivarRoleDoCargo()` (void, no-arg) sets `this.role` to one of `"ESTAGIARIO"`, `"TECNICO"`, `"ADMIN"`. `UsuarioService.criarUsuario()`/`atualizarUsuario()` already call this method — no change needed there, they pick up the new behavior automatically.

This is the current (pre-change) state of the two things being modified:

```java
// Usuario.java:32
private String role = "CLIENTE";

// Usuario.java:65-72
// Deriva o role de segurança a partir do cargo organizacional: ESTAGIARIO (ou sem cargo) = CLIENTE, qualquer outro = ADMIN.
public void derivarRoleDoCargo() {
    if (cargo != null && !cargo.equalsIgnoreCase("ESTAGIARIO")) {
        this.role = "ADMIN";
    } else {
        this.role = "CLIENTE";
    }
}
```

```java
// UsuarioTest.java:11-39
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
```

- [ ] **Step 1: Update the 3 existing tests to the new expected values, and add a 4th test for `GESTOR`**

Replace `UsuarioTest.java:11-39` (the three `derivarRoleDoCargo_*` tests) with:

```java
    @Test
    void derivarRoleDoCargo_semCargo_viraEstagiario() {
        Usuario usuario = new Usuario();
        usuario.setCargo(null);

        usuario.derivarRoleDoCargo();

        assertEquals("ESTAGIARIO", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_estagiario_viraEstagiario() {
        Usuario usuario = new Usuario();
        usuario.setCargo("estagiario");

        usuario.derivarRoleDoCargo();

        assertEquals("ESTAGIARIO", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_gestor_viraTecnico() {
        Usuario usuario = new Usuario();
        usuario.setCargo("GESTOR");

        usuario.derivarRoleDoCargo();

        assertEquals("TECNICO", usuario.getRole());
    }

    @Test
    void derivarRoleDoCargo_diretorCem_viraAdmin() {
        Usuario usuario = new Usuario();
        usuario.setCargo("DIRETOR_CEM");

        usuario.derivarRoleDoCargo();

        assertEquals("ADMIN", usuario.getRole());
    }
```

Leave `validarSenhaObrigatoria_*` tests (lines 41-64 of the original file) untouched — they're unrelated to this change.

- [ ] **Step 2: Run tests to verify they fail**

Run (from `pilot/`): `mvn test -Dtest=UsuarioTest`
Expected: `derivarRoleDoCargo_semCargo_viraEstagiario`, `derivarRoleDoCargo_estagiario_viraEstagiario`, `derivarRoleDoCargo_diretorCem_viraAdmin` FAIL (actual value `ADMIN`/`CLIENTE` from old logic vs. expected `ESTAGIARIO`/`ADMIN`); `derivarRoleDoCargo_gestor_viraTecnico` FAILS with actual `ADMIN` vs expected `TECNICO`.

- [ ] **Step 3: Implement the three-way mapping**

In `Usuario.java`, change the default field value on line 32:

```java
    private String role = "ESTAGIARIO";
```

And replace the method at lines 65-72:

```java
    // Deriva o role de segurança a partir do cargo organizacional:
    // GESTOR = TECNICO, ESTAGIARIO (ou sem cargo) = ESTAGIARIO, qualquer outro (ex.: DIRETOR_CEM) = ADMIN.
    public void derivarRoleDoCargo() {
        if (cargo == null || cargo.equalsIgnoreCase("ESTAGIARIO")) {
            this.role = "ESTAGIARIO";
        } else if (cargo.equalsIgnoreCase("GESTOR")) {
            this.role = "TECNICO";
        } else {
            this.role = "ADMIN";
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run (from `pilot/`): `mvn test -Dtest=UsuarioTest`
Expected: all 7 tests in `UsuarioTest` PASS (4 `derivarRoleDoCargo_*` + 3 `validarSenhaObrigatoria_*`).

- [ ] **Step 5: Commit**

```bash
git add pilot/src/main/java/com/zeiss/pilot/entity/Usuario.java pilot/src/test/java/com/zeiss/pilot/entity/UsuarioTest.java
git commit -m "feat(usuario): map GESTOR cargo to TECNICO role, ESTAGIARIO cargo to ESTAGIARIO role

Corrects a pre-existing bug where GESTOR was granted full ADMIN
authority it never actually had (frontend Auth.PERMS already treated
GESTOR as a lesser role). Also renames the CLIENTE role literal to
ESTAGIARIO since no external-client user type exists in this system
(deliberate divergence from TCC's RF11/RNF02 CLIENTE terminology,
documented in docs/superpowers/specs/2026-08-17-papel-tecnico-design.md)."
```

---

### Task 2: SecurityConfig — restrict writes on maquinas/servicos to ADMIN/TECNICO

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java:24-35`

**Interfaces:**
- Consumes: `Usuario.role` values produced by Task 1 (`"TECNICO"`, `"ADMIN"`, `"ESTAGIARIO"`) — Spring Security's `hasAnyRole("ADMIN", "TECNICO")` matches authorities `ROLE_ADMIN` / `ROLE_TECNICO`, which `CustomUserDetailsService` already produces unchanged.
- Produces: nothing consumed by later tasks — this is the last code change. Task 3 verifies its effect over HTTP.

No test infrastructure exists for `SecurityConfig` in this repo (no `SecurityConfigTest`, no `@WebMvcTest` for `MaquinaController`/`ServicoController`) — per the spec's "Testes" section, this task is verified manually via `curl` in Task 3, not via a JUnit test. Do not introduce new test infrastructure for this — out of scope for B2.

- [ ] **Step 1: Add the four new requestMatchers, before the generic `/api/**` fallback**

In `SecurityConfig.java`, the current `authorizeHttpRequests` block (lines 24-35) is:

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
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
```

Replace it with:

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

- [ ] **Step 2: Compile and run the full test suite to confirm no regression**

Run (from `pilot/`): `mvn test`
Expected: BUILD SUCCESS, same test count as before Task 1 plus the 4 `UsuarioTest` changes — no test touches `SecurityConfig` directly, so this step is a regression check, not a new-behavior check.

- [ ] **Step 3: Commit**

```bash
git add pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java
git commit -m "feat(security): require ADMIN or TECNICO role to write to /api/maquinas and /api/servicos

GET stays open to any authenticated user. Satisfies B2's done criterion:
at least one authorization rule now concretely differentiates TECNICO
from ESTAGIARIO (the renamed CLIENTE role)."
```

---

### Task 3: Manual curl verification

**Files:** none (no code changes — this task exercises the running application)

**Interfaces:**
- Consumes: the running Spring Boot app (`pilot/`, default port 8090 per `application.properties:8`), backed by the local Postgres dev DB, plus the ADMIN test login `admin@zeiss.com` / `SENHA_DE_TESTE` (already seeded — see repo memory `project_refactor_phase2_round1_status`).
- Produces: nothing for later tasks — this is the final acceptance check for B2's done criterion.

This task confirms over real HTTP what Tasks 1-2 changed in code: a `TECNICO` user can write to `/api/maquinas` and `/api/servicos`, an `ESTAGIARIO` user cannot (403), and `GET` stays open to both.

- [ ] **Step 1: Start the app**

Run (from `pilot/`): `mvn spring-boot:run`
Wait for `Started PilotApplication` in the log. Leave it running in this terminal; run the rest of the steps from a second terminal.

- [ ] **Step 2: Log in as ADMIN and capture the session cookie**

```bash
curl -c cookies-admin.txt -X POST http://localhost:8090/login \
  -d "username=admin@zeiss.com&password=SENHA_DE_TESTE" \
  -L -o /dev/null -s -w "%{http_code}\n"
```

Expected: `200` (formLogin redirects to `/index` on success, which returns 200).

- [ ] **Step 3: As ADMIN, create one TECNICO test user (cargo GESTOR) and one ESTAGIARIO test user**

```bash
curl -b cookies-admin.txt -X POST http://localhost:8090/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nome":"Tecnico Teste B2","email":"tecnico.b2@zeiss.com","senha":"SENHA_DE_TESTE","cargo":"GESTOR"}' \
  -s | grep -o '"role":"[^"]*"'

curl -b cookies-admin.txt -X POST http://localhost:8090/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nome":"Estagiario Teste B2","email":"estagiario.b2@zeiss.com","senha":"SENHA_DE_TESTE","cargo":"ESTAGIARIO"}' \
  -s | grep -o '"role":"[^"]*"'
```

Expected: first prints `"role":"TECNICO"`, second prints `"role":"ESTAGIARIO"`. If either doesn't match, stop — Task 1's logic or Task 3's cargo string is wrong; do not proceed to the next step.

- [ ] **Step 4: Log in as each test user, capturing separate cookie jars**

```bash
curl -c cookies-tecnico.txt -X POST http://localhost:8090/login \
  -d "username=tecnico.b2@zeiss.com&password=SENHA_DE_TESTE" \
  -L -o /dev/null -s -w "%{http_code}\n"

curl -c cookies-estagiario.txt -X POST http://localhost:8090/login \
  -d "username=estagiario.b2@zeiss.com&password=SENHA_DE_TESTE" \
  -L -o /dev/null -s -w "%{http_code}\n"
```

Expected: both print `200`.

- [ ] **Step 5: Confirm GET stays open to both roles**

```bash
curl -b cookies-tecnico.txt http://localhost:8090/api/maquinas -s -o /dev/null -w "TECNICO GET maquinas: %{http_code}\n"
curl -b cookies-estagiario.txt http://localhost:8090/api/maquinas -s -o /dev/null -w "ESTAGIARIO GET maquinas: %{http_code}\n"
curl -b cookies-tecnico.txt "http://localhost:8090/api/servicos?page=0&size=10" -s -o /dev/null -w "TECNICO GET servicos: %{http_code}\n"
curl -b cookies-estagiario.txt "http://localhost:8090/api/servicos?page=0&size=10" -s -o /dev/null -w "ESTAGIARIO GET servicos: %{http_code}\n"
```

Expected: all four print `200`.

- [ ] **Step 6: Confirm POST is allowed for TECNICO, forbidden for ESTAGIARIO**

```bash
curl -b cookies-tecnico.txt -X POST http://localhost:8090/api/maquinas \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maquina Teste B2 Tecnico"}' \
  -s -o /dev/null -w "TECNICO POST maquinas: %{http_code}\n"

curl -b cookies-estagiario.txt -X POST http://localhost:8090/api/maquinas \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maquina Teste B2 Estagiario"}' \
  -s -o /dev/null -w "ESTAGIARIO POST maquinas: %{http_code}\n"

curl -b cookies-tecnico.txt -X POST http://localhost:8090/api/servicos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Cliente Teste B2","solicitacao":"Teste B2","quantidade":1,"status":"Pendente","valor":100.00,"dataCriacao":"2026-08-17"}' \
  -s -o /dev/null -w "TECNICO POST servicos: %{http_code}\n"

curl -b cookies-estagiario.txt -X POST http://localhost:8090/api/servicos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Cliente Teste B2","solicitacao":"Teste B2","quantidade":1,"status":"Pendente","valor":100.00,"dataCriacao":"2026-08-17"}' \
  -s -o /dev/null -w "ESTAGIARIO POST servicos: %{http_code}\n"
```

Expected: both TECNICO POSTs print `200`; both ESTAGIARIO POSTs print `403`.

- [ ] **Step 7: Clean up test data created in Steps 3 and 6**

Note the `id` values returned by the TECNICO POSTs in Step 6 (or list via `curl -b cookies-admin.txt http://localhost:8090/api/maquinas -s` / `.../api/servicos?page=0&size=50` and find the ones named "Maquina Teste B2 Tecnico" / with `cliente":"Cliente Teste B2"`), then delete them and the two test users as ADMIN:

```bash
# Replace <maquina_id>, <servico_id> with the ids from Step 6's response bodies
curl -b cookies-admin.txt -X DELETE http://localhost:8090/api/maquinas/<maquina_id> -s -o /dev/null -w "cleanup maquina: %{http_code}\n"
curl -b cookies-admin.txt -X DELETE http://localhost:8090/api/servicos/<servico_id> -s -o /dev/null -w "cleanup servico: %{http_code}\n"

# Find the two test user ids
curl -b cookies-admin.txt http://localhost:8090/api/usuarios -s | grep -o '"id":[0-9]*,"nome":"[^"]*Teste B2[^"]*"'

# Replace <tecnico_user_id>, <estagiario_user_id>
curl -b cookies-admin.txt -X DELETE http://localhost:8090/api/usuarios/<tecnico_user_id> -s -o /dev/null -w "cleanup tecnico user: %{http_code}\n"
curl -b cookies-admin.txt -X DELETE http://localhost:8090/api/usuarios/<estagiario_user_id> -s -o /dev/null -w "cleanup estagiario user: %{http_code}\n"
```

Expected: all four cleanup calls print `204`.

- [ ] **Step 8: Stop the app and remove cookie jars**

Stop the `mvn spring-boot:run` process (Ctrl+C in its terminal), then:

```bash
rm -f cookies-admin.txt cookies-tecnico.txt cookies-estagiario.txt
```

- [ ] **Step 9: Record the result**

No commit for this task (no files changed). If every expected HTTP status in Steps 5-6 matched, B2's done criterion is met: report this to the user directly — do not silently proceed to `finishing-a-development-branch` without their confirmation, since this is the acceptance gate for the whole feature.

---

## Self-Review Notes

- **Spec coverage:** Mapeamento cargo→role → Task 1. Nova regra de autorização → Task 2. Testes (unitário + manual/curl) → Task 1 Steps 1-4 and Task 3. Fora de escopo items are called out as constraints/non-goals, no task touches them.
- **Placeholder scan:** the only bracketed placeholders are `<maquina_id>` / `<servico_id>` / `<tecnico_user_id>` / `<estagiario_user_id>` in Task 3 Step 7, which are inherently runtime-determined (server-generated IDs) — not a fillable-in-advance omission.
- **Type consistency:** `Usuario.role` stays a `String` throughout; the three literal values (`"ESTAGIARIO"`, `"TECNICO"`, `"ADMIN"`) used in Task 1's implementation match exactly what Task 2's `hasAnyRole("ADMIN", "TECNICO")` and Task 3's curl assertions check for.
