package com.zeiss.pilot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zeiss.pilot.entity.DocumentoMaquina;
import com.zeiss.pilot.entity.Maquina;
import com.zeiss.pilot.repository.DocumentoMaquinaRepository;
import com.zeiss.pilot.repository.MaquinaRepository;

@Service
public class DocumentoMaquinaService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoMaquinaService.class);

    private final DocumentoMaquinaRepository repository;

    private final MaquinaRepository maquinaRepository;

    private static final String BASE_PATH = "C:/PDFs/Maquina";

    public DocumentoMaquinaService(DocumentoMaquinaRepository repository, MaquinaRepository maquinaRepository) {
        this.repository = repository;
        this.maquinaRepository = maquinaRepository;
    }

    @Transactional
    public DocumentoMaquina upload(MultipartFile file, Long maquinaId,
                                      LocalDate dataExpiracao, String tipoDocumento) throws IOException {
        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new IllegalArgumentException("Máquina não encontrada: " + maquinaId));

        String pasta = BASE_PATH + maquinaId;
        Files.createDirectories(Paths.get(pasta));

        String nomeArquivo = file.getOriginalFilename();
        if (nomeArquivo == null || nomeArquivo.isBlank()) nomeArquivo = "documento.pdf";

        Path destino = Paths.get(pasta + "/" + nomeArquivo);
        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        DocumentoMaquina doc = new DocumentoMaquina();
        doc.setMaquina(maquina);
        doc.setNomeArquivo(nomeArquivo);
        doc.setCaminhoArquivo(destino.toString());
        doc.setTipoDocumento(tipoDocumento);
        doc.setDataUpload(LocalDateTime.now());
        doc.setDataExpiracao(dataExpiracao);
        doc.recalcularStatus();

        return repository.save(doc);
    }

    public List<DocumentoMaquina> listar(Long maquinaId) {
        return repository.findByMaquinaIdOrderByDataUploadDesc(maquinaId)
                .stream()
                .peek(DocumentoMaquina::recalcularStatus)
                .collect(Collectors.toList());
    }

    @Transactional
    public void remover(Long id) {
        repository.findById(id).ifPresent(doc -> {
            try {
                Files.deleteIfExists(Paths.get(doc.getCaminhoArquivo()));
            } catch (IOException e) {
                log.warn("Falha ao excluir arquivo físico do documento {} ({})", id, doc.getCaminhoArquivo(), e);
            }
            repository.deleteById(id);
        });
    }

    public ResponseEntity<Resource> abrir(Long id) {
        DocumentoMaquina doc = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado: " + id));
        Path path = Paths.get(doc.getCaminhoArquivo());
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNomeArquivo() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

}
