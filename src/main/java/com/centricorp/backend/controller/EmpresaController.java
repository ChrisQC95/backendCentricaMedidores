package com.centricorp.backend.controller;

import com.centricorp.backend.dto.EmpresaDTO;
import com.centricorp.backend.service.EmpresaService;
import com.centricorp.backend.service.EmpresaExcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD de Empresas.
 * Todas las rutas bajo /api/** están protegidas por JWT (ver SecurityConfig).
 *
 * GET    /api/empresas?page=0&size=10  → lista paginada
 * GET    /api/empresas/{ruc}           → obtener por RUC
 * POST   /api/empresas                 → crear nueva empresa
 * PUT    /api/empresas/{ruc}           → actualizar razón social
 * DELETE /api/empresas/{ruc}           → eliminar empresa
 */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmpresaService empresaService;
    private final EmpresaExcelService empresaExcelService;

    @GetMapping
    public ResponseEntity<Page<EmpresaDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(empresaService.findAll(normalizePage(page), normalizeSize(size)));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmpresaDTO>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(empresaService.search(q, normalizePage(page), normalizeSearchSize(size)));
    }

    @GetMapping("/{ruc}")
    public ResponseEntity<EmpresaDTO> findById(@PathVariable String ruc) {
        return ResponseEntity.ok(empresaService.findById(ruc));
    }

    @PostMapping
    public ResponseEntity<EmpresaDTO> create(@Valid @RequestBody EmpresaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.create(dto));
    }

    @PutMapping("/{ruc}")
    public ResponseEntity<EmpresaDTO> update(
            @PathVariable String ruc,
            @Valid @RequestBody EmpresaDTO dto
    ) {
        return ResponseEntity.ok(empresaService.update(ruc, dto));
    }

    @DeleteMapping("/{ruc}")
    public ResponseEntity<Void> delete(@PathVariable String ruc) {
        empresaService.delete(ruc);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/plantilla")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> descargarPlantilla() {
        java.io.ByteArrayInputStream stream = empresaExcelService.generarPlantillaExcel();
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=plantilla_empresas.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.InputStreamResource(stream));
    }

    @PostMapping("/upload")
    public ResponseEntity<com.centricorp.backend.dto.CargaMasivaResponseDTO> uploadEmpresas(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        com.centricorp.backend.dto.CargaMasivaResponseDTO response = empresaExcelService.procesarCargaMasiva(file);
        return ResponseEntity.ok(response);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private int normalizeSearchSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
