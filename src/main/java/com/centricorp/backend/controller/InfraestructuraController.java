package com.centricorp.backend.controller;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;
import com.centricorp.backend.service.InfraestructuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de Infraestructura + endpoint especial por empresa.
 *
 * GET    /api/infraestructura                    → listar todo
 * GET    /api/infraestructura/{id}               → obtener por ID
 * GET    /api/infraestructura/empresa/{ruc}      → lista plana por empresa (para selectores)
 * POST   /api/infraestructura                    → crear nodo
 * PUT    /api/infraestructura/{id}               → actualizar nodo
 * DELETE /api/infraestructura/{id}               → eliminar nodo (CASCADE a hijos y medidores)
 */
@RestController
@RequestMapping("/api/infraestructura")
@RequiredArgsConstructor
public class InfraestructuraController {

    private final InfraestructuraService infraService;

    @GetMapping
    public ResponseEntity<List<InfraestructuraResponseDTO>> findAll() {
        return ResponseEntity.ok(infraService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InfraestructuraResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(infraService.findById(id));
    }

    /**
     * Endpoint para que el frontend cargue el selector de infraestructura
     * filtrado por empresa. Retorna lista plana ordenada por nombre.
     */
    @GetMapping("/empresa/{ruc}")
    public ResponseEntity<List<InfraestructuraResponseDTO>> findByEmpresa(@PathVariable String ruc) {
        return ResponseEntity.ok(infraService.findByEmpresaRuc(ruc));
    }

    @PostMapping
    public ResponseEntity<InfraestructuraResponseDTO> create(
            @RequestBody InfraestructuraRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(infraService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InfraestructuraResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody InfraestructuraRequestDTO dto
    ) {
        return ResponseEntity.ok(infraService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        infraService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
