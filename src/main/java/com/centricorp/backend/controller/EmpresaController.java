package com.centricorp.backend.controller;

import com.centricorp.backend.dto.EmpresaDTO;
import com.centricorp.backend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de Empresas.
 * Todas las rutas bajo /api/** están protegidas por JWT (ver SecurityConfig).
 *
 * GET    /api/empresas          → listar todas
 * GET    /api/empresas/{ruc}    → obtener por RUC
 * POST   /api/empresas          → crear nueva empresa
 * PUT    /api/empresas/{ruc}    → actualizar razon social
 * DELETE /api/empresas/{ruc}    → eliminar empresa
 */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<EmpresaDTO>> findAll() {
        return ResponseEntity.ok(empresaService.findAll());
    }

    @GetMapping("/{ruc}")
    public ResponseEntity<EmpresaDTO> findById(@PathVariable String ruc) {
        return ResponseEntity.ok(empresaService.findById(ruc));
    }

    @PostMapping
    public ResponseEntity<EmpresaDTO> create(@RequestBody EmpresaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.create(dto));
    }

    @PutMapping("/{ruc}")
    public ResponseEntity<EmpresaDTO> update(
            @PathVariable String ruc,
            @RequestBody EmpresaDTO dto
    ) {
        return ResponseEntity.ok(empresaService.update(ruc, dto));
    }

    @DeleteMapping("/{ruc}")
    public ResponseEntity<Void> delete(@PathVariable String ruc) {
        empresaService.delete(ruc);
        return ResponseEntity.noContent().build();
    }
}
