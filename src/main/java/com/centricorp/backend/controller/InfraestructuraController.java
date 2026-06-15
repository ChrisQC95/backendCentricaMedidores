package com.centricorp.backend.controller;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;
import com.centricorp.backend.service.InfraestructuraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de Infraestructura + endpoint especial por empresa.
 *
 * GET    /api/infraestructura?page=0&size=10   → lista paginada
 * GET    /api/infraestructura/{id}             → obtener por ID
 * GET    /api/infraestructura/empresa/{ruc}    → lista plana por empresa (selectores)
 * POST   /api/infraestructura                  → crear nodo
 * PUT    /api/infraestructura/{id}             → actualizar nodo
 * DELETE /api/infraestructura/{id}             → eliminar nodo (CASCADE a hijos y medidores)
 */
@RestController
@RequestMapping("/api/infraestructura")
@RequiredArgsConstructor
public class InfraestructuraController {

    private final InfraestructuraService infraService;

    @GetMapping
    public ResponseEntity<Page<InfraestructuraResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(infraService.findAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InfraestructuraResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(infraService.findById(id));
    }

    /**
     * Lista plana (sin paginación) para uso en selectores/combo boxes del frontend.
     */
    @GetMapping("/empresa/{ruc}")
    public ResponseEntity<List<InfraestructuraResponseDTO>> findByEmpresa(@PathVariable String ruc) {
        return ResponseEntity.ok(infraService.findByEmpresaRuc(ruc));
    }

    @PostMapping
    public ResponseEntity<InfraestructuraResponseDTO> create(
            @Valid @RequestBody InfraestructuraRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(infraService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InfraestructuraResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody InfraestructuraRequestDTO dto
    ) {
        return ResponseEntity.ok(infraService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        infraService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
