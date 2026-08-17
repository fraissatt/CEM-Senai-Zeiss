# Papel TÉCNICO no Controle de Acesso — Design

**Item do backlog:** B2 (`docs/ENTREGA1_BACKLOG_E_SPRINT1.md`) — Sprint 1, prioridade 3.

**Critério de pronto (do backlog):** `Usuario` pode assumir o papel `TÉCNICO`, e ao menos uma regra de autorização (`SecurityConfig` ou `@PreAuthorize`) diferencia esse perfil de `CLIENTE`.

## Contexto

O TCC (RF11, RNF02, Especificação de Interface) especifica 3 perfis: `ADMIN`, `TÉCNICO`, `CLIENTE`. O código hoje só deriva `ADMIN` ou `CLIENTE` a partir do `cargo` organizacional, em `Usuario.derivarRoleDoCargo()`:

```java
public void derivarRoleDoCargo() {
    if (cargo != null && !cargo.equalsIgnoreCase("ESTAGIARIO")) {
        this.role = "ADMIN";
    } else {
        this.role = "CLIENTE";
    }
}
```

Isso agrupa `GESTOR` e `DIRETOR_CEM` como `ADMIN`, sem distinção. O frontend (`Auth.PERMS` em `core.js`), porém, **já trata `GESTOR` como um nível intermediário** — pode criar/editar/excluir mas não gerencia usuários (`viewUsuarios: false`) — a mesma distinção que separa `TÉCNICO` de `ADMIN` no TCC. Ou seja: o cargo `GESTOR` nunca teve permissão real de admin completo; `derivarRoleDoCargo()` está incorreto desde antes deste trabalho, não é um comportamento a preservar.

Confirmado com o usuário: na organização real, quem exerce a função de "Gestor" é quem opera as máquinas — não existe (nem vai existir, por ora) um cargo distinto de "técnico de laboratório". Também confirmado: o sistema terá só 3 tipos de usuário (Diretor/Admin, Gestor/Técnico, Estagiário), então não é necessário um 4º cargo.

## Mapeamento cargo → role

| Cargo | Role hoje | Role novo | Motivo |
|---|---|---|---|
| `ESTAGIARIO` (ou vazio) | `CLIENTE` | **`ESTAGIARIO`** | decisão deliberada do usuário — ver "Divergência do RF11/RNF02" abaixo |
| `GESTOR` | `ADMIN` | **`TECNICO`** | corrige mapeamento incorreto; permissão real do Gestor nunca incluiu `viewUsuarios` |
| `DIRETOR_CEM` | `ADMIN` | `ADMIN` | sem mudança — admin geral absoluto |

`Usuario.role` tem valor padrão de campo `"CLIENTE"` — nunca observado na prática (`UsuarioService.criarUsuario()`/`atualizarUsuario()` sempre chamam `derivarRoleDoCargo()` antes de persistir), mas atualizado para `"ESTAGIARIO"` por consistência, já que o literal antigo não existirá mais em lugar nenhum do sistema.

### ⚠️ Divergência deliberada do RF11/RNF02

O TCC especifica o terceiro perfil como `CLIENTE`. O usuário decidiu conscientemente usar `ESTAGIARIO` em vez disso, porque não existe hoje (nem está planejado) um usuário "cliente" externo real — todo usuário desse nível é, na prática, um estagiário. **Isso precisa ser reconciliado com a redação do TCC/RF11/RNF02 antes da entrega final**, ou justificado explicitamente na apresentação — não é um esquecimento, é uma escolha registrada aqui para não se perder.

## Nova regra de autorização

Em `SecurityConfig.java`, adicionar restrição de escrita em `/api/maquinas/**` e `/api/servicos/**`:

```java
.requestMatchers(HttpMethod.POST, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
.requestMatchers(HttpMethod.PUT, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
.requestMatchers(HttpMethod.PATCH, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
.requestMatchers(HttpMethod.DELETE, "/api/maquinas/**", "/api/servicos/**").hasAnyRole("ADMIN", "TECNICO")
```

Posicionadas antes do fallback genérico `.requestMatchers("/api/**").authenticated()`, para que tenham precedência (Spring Security avalia `requestMatchers` na ordem declarada, primeira que casar vence).

`GET` continua liberado pra qualquer usuário autenticado — `ESTAGIARIO` continua vendo máquinas/serviços, só não cria/edita/exclui. Isso satisfaz o critério de pronto: `TECNICO` e `ESTAGIARIO` (o novo nome de `CLIENTE`) ficam concretamente diferenciados por pelo menos uma regra testável.

`CustomUserDetailsService` já monta a authority como `"ROLE_" + usuario.getRole()` de forma genérica (não hardcoded pra ADMIN/CLIENTE) — nenhuma mudança necessária ali para o novo valor `TECNICO` funcionar com `hasAnyRole`.

**Escopo do wildcard `/api/maquinas/**` confirmado deliberadamente amplo:** o padrão também cobre os sub-endpoints aninhados de sessões (`/{id}/sessoes`), manutenções (`/{id}/manutencoes`) e agendamentos (`/{id}/agendamentos`) — não só o CRUD da máquina em si. Isso significa que `ESTAGIARIO` também fica bloqueado (403) de registrar sessão de uso (ligar/desligar máquina) e de criar agendamento, mesmo que o frontend (`Auth.PERMS` em `core.js`) hoje conceda `create: true` pra `ESTAGIARIO` nessas ações. Achado pela revisão final do branch (2026-08-17) e confirmado com o usuário: comportamento mantido como está — bloqueado — por ser a leitura literal correta do padrão que este spec já pedia, não uma regressão a corrigir.

## Limitação conhecida: role não é recalculada no login

`derivarRoleDoCargo()` só roda em `UsuarioService.criarUsuario()` e `atualizarUsuario()` — nunca em `CustomUserDetailsService.loadUserByUsername()`. Ou seja, a coluna `role` gravada no banco é o que vale no login, não um valor recalculado a cada vez a partir do `cargo`.

Isso significa que qualquer usuário cujo `cargo` mude (ou cujo `role` tenha sido gravado por uma versão anterior desta lógica) só recebe a role correta na próxima vez que for salvo pela tela de Usuários — não automaticamente. Achado pela revisão final do branch (2026-08-17); confirmado com o usuário que o banco de dev atual tem só 2 usuários, ambos já `ADMIN` (papel que não muda nesta revisão), então não há nenhuma linha afetada agora. Decisão: documentar como limitação conhecida, sem mudança de código — se este sistema ganhar uma base de usuários maior antes de produção, ou se um `GESTOR` for cadastrado por fora do fluxo normal do app, vale revisitar (opção mais robusta: mover a chamada de `derivarRoleDoCargo()` pra dentro de `CustomUserDetailsService`, tornando a coluna um cache em vez de fonte da verdade).

## Fora de escopo

- **`Auth` em `core.js`** (sistema de permissões do frontend, menu lateral, badges): `Auth.set()` nunca é chamado em lugar nenhum do código — o módulo sempre cai no default hardcoded `'GESTOR'`, independente de quem faz login. É um bug pré-existente, não introduzido nem agravado por este trabalho. O critério de pronto do backlog é especificamente sobre backend (`SecurityConfig`/`@PreAuthorize`), então fica de fora aqui.
- Nenhum outro endpoint (`/api/amostras`, `/api/editais` etc.) é tocado — só os dois módulos que o usuário nomeou explicitmente (máquinas, serviços).
- Dropdown de `cargo` na tela Usuários (`usuarios.html`) não precisa de opção nova — os 3 cargos já existentes continuam sendo os únicos.

## Testes

1. **Unitário:** `derivarRoleDoCargo()` — 3 casos (`ESTAGIARIO`→`ESTAGIARIO`, `GESTOR`→`TECNICO`, `DIRETOR_CEM`→`ADMIN`), seguindo o padrão dos testes já existentes na Fase 2 Rodada 1 (`UsuarioTest`).
2. **Manual/curl:** com um usuário de cada cargo autenticado, confirmar por HTTP status: `GET /api/maquinas` e `GET /api/servicos` → 200 pros 3; `POST`/`PUT`/`DELETE` nesses endpoints → 200/204 pra `TECNICO` e `ADMIN`, 403 pra `ESTAGIARIO`.
