# Frontend Integration Phase 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir os arquivos de frontend antigos (HTML/CSS/JS) pelo novo frontend redesenhado e corrigir as rotas da API do backend para casar com o padrão `/api/[recurso]` esperado pelo novo frontend.

**Architecture:** O projeto é um monolito Spring Boot com Thymeleaf. O novo frontend usa o mesmo stack (vanilla JS + HTML), então a integração consiste em: (1) copiar os arquivos estáticos e templates; (2) renomear as rotas REST de `/[recurso]/api` para `/api/[recurso]`; (3) atualizar PageController e SecurityConfig para refletir as novas páginas e regras.

**Tech Stack:** Java 21, Spring Boot 3.4.4, Spring Security, Thymeleaf, PostgreSQL, Vanilla JS, CSS3.

---

## Mapeamento de arquivos

### Criar
- `pilot/src/main/java/com/zeiss/pilot/controller/RelatorioController.java` — endpoints de relatório/dashboard que precisam manter o path legado

### Modificar
- `pilot/src/main/java/com/zeiss/pilot/controller/ProjetoController.java` — renomear rotas
- `pilot/src/main/java/com/zeiss/pilot/controller/EventoController.java` — renomear rotas, remover page rendering
- `pilot/src/main/java/com/zeiss/pilot/controller/ServicoController.java` — renomear rotas, remover page rendering
- `pilot/src/main/java/com/zeiss/pilot/controller/VisitaTecnicaController.java` — renomear rotas, remover page rendering
- `pilot/src/main/java/com/zeiss/pilot/controller/EditalController.java` — renomear rotas, remover page rendering
- `pilot/src/main/java/com/zeiss/pilot/controller/UsuarioController.java` — renomear rotas
- `pilot/src/main/java/com/zeiss/pilot/controller/DocumentoPDFController.java` — adicionar `GET /api/documentos`
- `pilot/src/main/java/com/zeiss/pilot/controller/PageController.java` — rotas de todas as páginas
- `pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java` — atualizar regras de acesso

### Sobrescrever/copiar (estáticos)
- `pilot/src/main/resources/static/css/` — substituir todo o conteúdo
- `pilot/src/main/resources/static/js/` — substituir todo o conteúdo
- `pilot/src/main/resources/static/img/` — adicionar imagens novas
- `pilot/src/main/resources/templates/` — substituir HTMLs existentes, adicionar novos

### Remover (não existem no novo frontend)
- `pilot/src/main/resources/templates/pastas.html`
- `pilot/src/main/resources/templates/documentosPorSubpasta.html`
- `pilot/src/main/resources/templates/padrao.html`

---

## Referência: Mapeamento de rotas API

| Recurso | Rota antiga (backend atual) | Rota nova (esperada pelo novo frontend) |
|---|---|---|
| Projetos CRUD | `/projetos/api/**` | `/api/projetos/**` |
| Eventos CRUD | `/eventos/api/**` | `/api/eventos/**` |
| Eventos relatório | `/eventos/relatorios` | `/eventos/relatorios` ✓ (não muda) |
| Serviços CRUD | `/servicos/api/**` | `/api/servicos/**` |
| Serviços relatório | `/servicos/relatorio-servicos/api` | `/servicos/relatorio-servicos/api` ✓ (não muda) |
| Visitas CRUD | `/visitas-tecnicas/api/**` | `/api/visitas-tecnicas/**` |
| Editais CRUD | `/editais/api/**` | `/api/editais/**` |
| Usuários CRUD | `/usuarios/api/**` | `/api/usuarios/**` |
| Documentos | `/api/documentos/**` | `/api/documentos/**` ✓ (não muda) |
| Documentos lista | *(não existe)* | `GET /api/documentos` (novo) |

---

## Task 1: Copiar arquivos CSS

**Files:**
- Apagar: `pilot/src/main/resources/static/css/` (conteúdo antigo)
- Copiar de: `C:\Users\João Vítor Mamede\Documents\Zeiss-Front\static\css\`
- Para: `pilot/src/main/resources/static/css\`

- [ ] **Step 1: Remover CSS antigos**

```powershell
Remove-Item -Recurse -Force "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\css\*"
```

- [ ] **Step 2: Copiar novos CSS**

```powershell
Copy-Item -Recurse -Force "C:\Users\João Vítor Mamede\Documents\Zeiss-Front\static\css\*" "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\css\"
```

- [ ] **Step 3: Verificar arquivos copiados**

```powershell
Get-ChildItem "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\css\"
```

Esperado: `design-system.css`, `layout.css`, `components.css`, `login.css` e mais arquivos específicos por página.

- [ ] **Step 4: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/resources/static/css/
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "chore: replace frontend CSS with new design system"
```

---

## Task 2: Copiar arquivos JavaScript

**Files:**
- Apagar: `pilot/src/main/resources/static/js/` (conteúdo antigo)
- Copiar de: `C:\Users\João Vítor Mamede\Documents\Zeiss-Front\static\js\`
- Para: `pilot/src/main/resources/static/js\`

- [ ] **Step 1: Remover JS antigos**

```powershell
Remove-Item -Recurse -Force "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\js\*"
```

- [ ] **Step 2: Copiar novos JS**

```powershell
Copy-Item -Recurse -Force "C:\Users\João Vítor Mamede\Documents\Zeiss-Front\static\js\*" "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\js\"
```

- [ ] **Step 3: Verificar**

```powershell
Get-ChildItem -Recurse "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\js\" | Select-Object Name
```

Esperado: `api.js`, `core.js`, `ui.js`, `i18n.js` na raiz e pasta `modules/` com os módulos de feature.

- [ ] **Step 4: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/resources/static/js/
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "chore: replace frontend JS with new modular structure"
```

---

## Task 3: Copiar imagens

**Files:**
- Copiar de: `C:\Users\João Vítor Mamede\Documents\Zeiss-Front\static\img\`
- Para: `pilot/src/main/resources/static/img\`

- [ ] **Step 1: Copiar imagens (sem apagar as antigas — pode haver imagens ainda em uso)**

```powershell
Copy-Item -Recurse -Force "C:\Users\João Vítor Mamede\Documents\Zeiss-Front\static\img\*" "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\img\"
```

- [ ] **Step 2: Verificar**

```powershell
Get-ChildItem "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\static\img\"
```

Esperado: logos Zeiss, logos SENAI, imagens de máquinas, qrcode-avaliacao.png.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/resources/static/img/
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "chore: add new frontend image assets"
```

---

## Task 4: Copiar e substituir templates HTML

**Files:**
- Copiar de: `C:\Users\João Vítor Mamede\Documents\Zeiss-Front\templates\`
- Para: `pilot/src/main/resources/templates\`

- [ ] **Step 1: Copiar todos os templates (sobrescreve os existentes)**

```powershell
Copy-Item -Recurse -Force "C:\Users\João Vítor Mamede\Documents\Zeiss-Front\templates\*" "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\templates\"
```

- [ ] **Step 2: Remover templates obsoletos** (não existem no novo frontend)

```powershell
Remove-Item -Force "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\templates\pastas.html"
Remove-Item -Force "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\templates\documentosPorSubpasta.html"
Remove-Item -Force "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\templates\padrao.html"
```

- [ ] **Step 3: Verificar templates presentes**

```powershell
Get-ChildItem "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot\src\main\resources\templates\" | Select-Object Name
```

Esperado: index.html, login.html, 404.html, projetos.html, usuarios.html, documentos.html, lista-eventos.html, dashboardEventos.html, dashboardServicos.html, servicos.html, visitasTecnicas.html, lista-editais.html, detalhes-edital.html, almoxarifado.html, amostras.html, avaliacao.html, dashboard-avaliacao.html, dashboard-estagiarios.html, kanban-estagiarios.html, maquinas.html, verificacao-ambiental.html, qrcode-avaliacao.html.

- [ ] **Step 4: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/resources/templates/
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "chore: replace HTML templates with new frontend design"
```

---

## Task 5: Refatorar ProjetoController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/ProjetoController.java`

Mudança: trocar `@RequestMapping("/projetos")` por `@RequestMapping("/api/projetos")` e remover o segmento `/api` dos métodos.

- [ ] **Step 1: Reescrever o controller**

Conteúdo final do arquivo:

```java
package com.zeiss.pilot.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.ProjetoDTO;
import com.zeiss.pilot.entity.Projeto;
import com.zeiss.pilot.service.ProjetoService;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping
    public ResponseEntity<List<ProjetoDTO>> listarProjetos() {
        return ResponseEntity.ok(projetoService.listarProjetos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoDTO> buscarPorId(@PathVariable Long id) {
        Optional<ProjetoDTO> projeto = projetoService.buscarPorId(id);
        return projeto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProjetoDTO> criarProjeto(@RequestBody Projeto projeto) {
        return ResponseEntity.ok(projetoService.criarProjeto(projeto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoDTO> atualizarProjeto(@PathVariable Long id, @RequestBody Projeto projeto) {
        ProjetoDTO atualizado = projetoService.atualizarProjeto(id, projeto);
        return (atualizado != null) ? ResponseEntity.ok(atualizado) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarProjeto(@PathVariable Long id) {
        return projetoService.excluirProjeto(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 2: Compilar para verificar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

Esperado: BUILD SUCCESS, sem erros.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/ProjetoController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: move ProjetoController API to /api/projetos"
```

---

## Task 6: Criar RelatorioController

**Files:**
- Create: `pilot/src/main/java/com/zeiss/pilot/controller/RelatorioController.java`

Estes dois endpoints mantêm paths legados que o novo frontend já usa corretamente (`/eventos/relatorios` e `/servicos/relatorio-servicos/api`). Ao separar em controller próprio, os controllers de Evento e Serviço ficam livres para usar `/api/eventos` e `/api/servicos`.

- [ ] **Step 1: Criar o arquivo**

```java
package com.zeiss.pilot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.EventoRelatorioDTO;
import com.zeiss.pilot.dto.RelatorioMensalDTO;
import com.zeiss.pilot.service.EventoService;
import com.zeiss.pilot.service.ServicoService;

@RestController
public class RelatorioController {

    private final EventoService eventoService;
    private final ServicoService servicoService;

    public RelatorioController(EventoService eventoService, ServicoService servicoService) {
        this.eventoService = eventoService;
        this.servicoService = servicoService;
    }

    @GetMapping("/eventos/relatorios")
    public ResponseEntity<EventoRelatorioDTO> getRelatoriosEventos() {
        return ResponseEntity.ok(eventoService.getRelatoriosEventos());
    }

    @GetMapping("/servicos/relatorio-servicos/api")
    public ResponseEntity<List<RelatorioMensalDTO>> getRelatorioServicos() {
        return ResponseEntity.ok(servicoService.obterRelatorioMensal());
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/RelatorioController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "feat: extract RelatorioController for dashboard data endpoints"
```

---

## Task 7: Refatorar EventoController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/EventoController.java`

Remove page rendering e `/eventos/relatorios` (agora em RelatorioController). Muda base para `/api/eventos`.

- [ ] **Step 1: Reescrever o controller**

```java
package com.zeiss.pilot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.EventoDTO;
import com.zeiss.pilot.service.EventoService;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public ResponseEntity<List<EventoDTO>> getAllEventos() {
        return ResponseEntity.ok(eventoService.getAllEventos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> getEventoById(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.getEventoById(id));
    }

    @PostMapping
    public ResponseEntity<EventoDTO> createEvento(@RequestBody EventoDTO eventoDTO) {
        return ResponseEntity.ok(eventoService.createEvento(eventoDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> updateEvento(@PathVariable Long id, @RequestBody EventoDTO eventoDTO) {
        return ResponseEntity.ok(eventoService.updateEvento(id, eventoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvento(@PathVariable Long id) {
        eventoService.deleteEvento(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/EventoController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: move EventoController API to /api/eventos"
```

---

## Task 8: Refatorar ServicoController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/ServicoController.java`

Remove page rendering e `/servicos/relatorio-servicos/api` (agora em RelatorioController). Muda base para `/api/servicos`.

- [ ] **Step 1: Reescrever o controller**

```java
package com.zeiss.pilot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.ServicoDTO;
import com.zeiss.pilot.service.ServicoService;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public ResponseEntity<List<ServicoDTO>> listarTodosServicos() {
        return ResponseEntity.ok(servicoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicoDTO> buscarServicoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ServicoDTO> criarServico(@RequestBody ServicoDTO servicoDTO) {
        if (servicoDTO.getObservacao() != null && servicoDTO.getObservacao().trim().isEmpty()) {
            servicoDTO.setObservacao(null);
        }
        return ResponseEntity.ok(servicoService.criarServico(servicoDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicoDTO> atualizarServico(@PathVariable Long id, @RequestBody ServicoDTO servicoDTO) {
        return ResponseEntity.ok(servicoService.atualizarServico(id, servicoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirServico(@PathVariable Long id) {
        servicoService.excluirServico(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/ServicoController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: move ServicoController API to /api/servicos"
```

---

## Task 9: Refatorar VisitaTecnicaController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/VisitaTecnicaController.java`

- [ ] **Step 1: Reescrever o controller**

```java
package com.zeiss.pilot.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.VisitaTecnicaDTO;
import com.zeiss.pilot.entity.VisitaTecnica;
import com.zeiss.pilot.service.VisitaTecnicaService;

@RestController
@RequestMapping("/api/visitas-tecnicas")
public class VisitaTecnicaController {

    private final VisitaTecnicaService service;

    public VisitaTecnicaController(VisitaTecnicaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<VisitaTecnicaDTO>> getAllVisitas() {
        List<VisitaTecnica> visitas = service.listarVisitas();
        List<VisitaTecnicaDTO> dtos = visitas.stream()
                                             .map(VisitaTecnicaDTO::fromEntity)
                                             .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<VisitaTecnicaDTO> salvarVisita(@RequestBody VisitaTecnicaDTO dto) {
        VisitaTecnica entidade = dto.toEntity();
        VisitaTecnica salvo = service.salvarVisita(entidade);
        return ResponseEntity.ok(VisitaTecnicaDTO.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitaTecnicaDTO> atualizarVisita(@PathVariable Long id, @RequestBody VisitaTecnicaDTO dto) {
        VisitaTecnica entidade = dto.toEntity();
        entidade.setId(id);
        VisitaTecnica atualizado = service.salvarVisita(entidade);
        return ResponseEntity.ok(VisitaTecnicaDTO.fromEntity(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarVisita(@PathVariable Long id) {
        service.deletarVisita(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/VisitaTecnicaController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: move VisitaTecnicaController API to /api/visitas-tecnicas"
```

---

## Task 10: Refatorar EditalController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/EditalController.java`

- [ ] **Step 1: Reescrever o controller**

```java
package com.zeiss.pilot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.EditalDTO;
import com.zeiss.pilot.service.EditalService;

@RestController
@RequestMapping("/api/editais")
public class EditalController {

    private final EditalService editalService;

    public EditalController(EditalService editalService) {
        this.editalService = editalService;
    }

    @GetMapping
    public ResponseEntity<List<EditalDTO>> listarTodos() {
        return ResponseEntity.ok(editalService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EditalDTO> buscarPorId(@PathVariable Long id) {
        EditalDTO edital = editalService.buscarPorId(id);
        return edital != null ? ResponseEntity.ok(edital) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<EditalDTO> criarEdital(@RequestBody EditalDTO editalDTO) {
        try {
            return ResponseEntity.ok(editalService.criarEdital(editalDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<EditalDTO> atualizarEdital(@PathVariable Long id, @RequestBody EditalDTO editalDTO) {
        try {
            editalDTO.setId(id);
            return ResponseEntity.ok(editalService.atualizarEdital(id, editalDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirEdital(@PathVariable Long id) {
        try {
            editalService.excluirEdital(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/EditalController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: move EditalController API to /api/editais"
```

---

## Task 11: Refatorar UsuarioController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/UsuarioController.java`

- [ ] **Step 1: Reescrever o controller**

```java
package com.zeiss.pilot.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zeiss.pilot.dto.UsuarioDTO;
import com.zeiss.pilot.entity.Usuario;
import com.zeiss.pilot.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios(@RequestParam(required = false) String role) {
        if (role != null && !role.isEmpty()) {
            return ResponseEntity.ok(usuarioService.listarUsuariosPorRole(role));
        }
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<UsuarioDTO>> listarApenasAdmins() {
        return ResponseEntity.ok(usuarioService.listarUsuariosPorRole("ADMIN"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> criarUsuario(@RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.criarUsuario(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.atualizarUsuario(id, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validar-senha-admin")
    public ResponseEntity<Boolean> validarSenhaAdmin(@RequestBody Map<String, String> payload, Principal principal) {
        String senhaDigitada = payload.get("senha");
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        return ResponseEntity.ok(passwordEncoder.matches(senhaDigitada, usuario.getSenha()));
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/UsuarioController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: move UsuarioController API to /api/usuarios"
```

---

## Task 12: Adicionar GET /api/documentos no DocumentoPDFController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/DocumentoPDFController.java`

O novo frontend chama `GET /api/documentos` para listar os documentos do usuário logado. O backend atual não tem esse endpoint simples — tem `GET /api/documentos/usuario/meus` (paginado). Adicionamos um alias.

- [ ] **Step 1: Adicionar o método no DocumentoPDFController**

Adicionar dentro da classe, após o método `listarPorUsuario`:

```java
@GetMapping
public ResponseEntity<List<DocumentoPDFDTO>> listarMeusDocumentosSemPaginacao() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    Usuario usuario = usuarioService.buscarPorEmail(email);
    return ResponseEntity.ok(service.listarPorUsuario(usuario.getId()));
}
```

O import necessário (adicionar ao topo do arquivo se não existir):
```java
import org.springframework.http.ResponseEntity;
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/DocumentoPDFController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "feat: add GET /api/documentos endpoint for frontend document listing"
```

---

## Task 13: Atualizar PageController

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/controller/PageController.java`

Adicionar rotas para todas as páginas existentes e novas. Remover rotas de páginas obsoletas (`/pastas`, `/documentosPorSubpasta`). As novas páginas de módulos (almoxarifado, amostras etc.) retornam o template sem dados de modelo — o JS busca via API.

- [ ] **Step 1: Reescrever o PageController**

```java
package com.zeiss.pilot.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.zeiss.pilot.repository.UsuarioRepository;

@Controller
public class PageController {

    private final UsuarioRepository usuarioRepository;

    public PageController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // ── Páginas principais ──────────────────────────────────────────────
    @GetMapping({"/", "/index"})
    public String paginaInicial() {
        return "index";
    }

    @GetMapping("/projetos")
    public String paginaProjetos() {
        return "projetos";
    }

    @GetMapping("/usuarios")
    public String paginaUsuarios() {
        return "usuarios";
    }

    @GetMapping("/documentos")
    public String paginaDocumentos(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        usuarioRepository.findByEmail(userDetails.getUsername())
                .ifPresent(u -> model.addAttribute("usuarioId", u.getId()));
        return "documentos";
    }

    // ── Eventos ─────────────────────────────────────────────────────────
    @GetMapping("/lista-eventos")
    public String paginaListaEventos() {
        return "lista-eventos";
    }

    @GetMapping("/dashboardEventos")
    public String paginaDashboardEventos() {
        return "dashboardEventos";
    }

    // ── Serviços ─────────────────────────────────────────────────────────
    @GetMapping("/servicos")
    public String paginaServicos() {
        return "servicos";
    }

    @GetMapping("/dashboardServicos")
    public String paginaDashboardServicos() {
        return "dashboardServicos";
    }

    // ── Visitas Técnicas ─────────────────────────────────────────────────
    @GetMapping("/visitasTecnicas")
    public String paginaVisitasTecnicas() {
        return "visitasTecnicas";
    }

    // ── Editais ──────────────────────────────────────────────────────────
    @GetMapping("/lista-editais")
    public String paginaListaEditais() {
        return "lista-editais";
    }

    @GetMapping("/detalhes-edital")
    public String paginaDetalhesEdital() {
        return "detalhes-edital";
    }

    // ── Novos módulos ────────────────────────────────────────────────────
    @GetMapping("/almoxarifado")
    public String paginaAlmoxarifado() {
        return "almoxarifado";
    }

    @GetMapping("/amostras")
    public String paginaAmostras() {
        return "amostras";
    }

    @GetMapping("/avaliacao")
    public String paginaAvaliacao() {
        return "avaliacao";
    }

    @GetMapping("/dashboard-avaliacao")
    public String paginaDashboardAvaliacao() {
        return "dashboard-avaliacao";
    }

    @GetMapping("/maquinas")
    public String paginaMaquinas() {
        return "maquinas";
    }

    @GetMapping("/verificacao-ambiental")
    public String paginaVerificacaoAmbiental() {
        return "verificacao-ambiental";
    }

    @GetMapping("/kanban-estagiarios")
    public String paginaKanbanEstagiarios() {
        return "kanban-estagiarios";
    }

    @GetMapping("/dashboard-estagiarios")
    public String paginaDashboardEstagiarios() {
        return "dashboard-estagiarios";
    }

    @GetMapping("/qrcode-avaliacao")
    public String paginaQrcodeAvaliacao() {
        return "qrcode-avaliacao";
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/controller/PageController.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "feat: update PageController with all new frontend page routes"
```

---

## Task 14: Atualizar SecurityConfig

**Files:**
- Modify: `pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java`

Mudanças necessárias:
- `/usuarios/**` → `/api/usuarios/**` (ADMIN)
- POST `/eventos/api` → POST `/api/eventos` (ADMIN)
- Remover regra redundante `/visitas-tecnicas/api/**` (coberta por `/api/**`)
- Adicionar `/avaliacao` e `/qrcode-avaliacao` como públicos (acesso via QR code sem login)

- [ ] **Step 1: Reescrever o SecurityConfig**

```java
package com.zeiss.pilot.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/css/**", "/img/**", "/js/**",
                    "/avaliacao", "/qrcode-avaliacao"
                ).permitAll()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/eventos").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/index", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 2: Compilar**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" add pilot/src/main/java/com/zeiss/pilot/security/SecurityConfig.java
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit -m "refactor: update SecurityConfig for new API paths and public pages"
```

---

## Task 15: Build final e smoke test

- [ ] **Step 1: Build completo**

```powershell
cd "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot\pilot"
.\mvnw.cmd package -DskipTests -q
```

Esperado: BUILD SUCCESS e arquivo JAR gerado em `target/`.

- [ ] **Step 2: Subir o servidor**

```powershell
.\mvnw.cmd spring-boot:run
```

Servidor sobe em `http://localhost:8090`.

- [ ] **Step 3: Checklist de páginas**

Abrir no browser e verificar que cada página carrega sem erro 404/500:

| URL | Template esperado |
|---|---|
| `http://localhost:8090/login` | login.html |
| `http://localhost:8090/` | index.html |
| `http://localhost:8090/projetos` | projetos.html |
| `http://localhost:8090/usuarios` | usuarios.html |
| `http://localhost:8090/servicos` | servicos.html |
| `http://localhost:8090/lista-eventos` | lista-eventos.html |
| `http://localhost:8090/dashboardEventos` | dashboardEventos.html |
| `http://localhost:8090/dashboardServicos` | dashboardServicos.html |
| `http://localhost:8090/visitasTecnicas` | visitasTecnicas.html |
| `http://localhost:8090/lista-editais` | lista-editais.html |
| `http://localhost:8090/detalhes-edital` | detalhes-edital.html |
| `http://localhost:8090/documentos` | documentos.html |
| `http://localhost:8090/almoxarifado` | almoxarifado.html |
| `http://localhost:8090/amostras` | amostras.html |
| `http://localhost:8090/avaliacao` | avaliacao.html (público) |
| `http://localhost:8090/qrcode-avaliacao` | qrcode-avaliacao.html (público) |
| `http://localhost:8090/maquinas` | maquinas.html |
| `http://localhost:8090/verificacao-ambiental` | verificacao-ambiental.html |
| `http://localhost:8090/dashboard-estagiarios` | dashboard-estagiarios.html |
| `http://localhost:8090/kanban-estagiarios` | kanban-estagiarios.html |
| `http://localhost:8090/dashboard-avaliacao` | dashboard-avaliacao.html |

- [ ] **Step 4: Checklist de APIs (via DevTools Network ou curl)**

Verificar que cada endpoint retorna dados (não 404):

```
GET  /api/projetos
GET  /api/eventos
GET  /api/servicos
GET  /api/visitas-tecnicas
GET  /api/editais
GET  /api/usuarios
GET  /api/documentos
GET  /eventos/relatorios
GET  /servicos/relatorio-servicos/api
```

- [ ] **Step 5: Commit final de integração**

```powershell
git -C "C:\Users\João Vítor Mamede\Downloads\Zeiss-Pilot" commit --allow-empty -m "chore: phase 1 frontend integration complete"
```

---

## Pontos de atenção pós-integração (Phase 2)

- **Módulos novos sem backend:** almoxarifado, amostras, avaliacoes, maquinas, estagiarios, kanban-cards, notas-estagiarios, verificacoes-ambientais — as páginas carregam mas as chamadas de API retornam 404. Implementar na Fase 2.
- **documentos.html:** O novo frontend usa `GET /api/documentos` para listar, mas o upload e a gestão de subpastas podem ter discrepâncias com o backend atual. Validar na Fase 2.
- **PastaDocumentoController:** O controller de pastas ainda existe no backend mas as páginas `pastas.html` e `documentosPorSubpasta.html` foram removidas do frontend. Avaliar se o controller pode ser removido ou se será reaproveitado.
