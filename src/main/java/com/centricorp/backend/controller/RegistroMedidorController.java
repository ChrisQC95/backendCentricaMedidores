package com.centricorp.backend.controller;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;
import com.centricorp.backend.service.RegistroMedidorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de Registro de Medidores.
 *
 * POST /api/medidores                              → insertar nuevo registro
 * GET  /api/medidores?page=0&size=10              → lista paginada (Server-Side)
 * GET  /api/medidores/reporte?mes=5&anio=2025     → registros filtrados (Excel)
 */
@RestController
@RequestMapping("/api/medidores")
@RequiredArgsConstructor
public class RegistroMedidorController {

    private final RegistroMedidorService registroService;

    /**
     * Inserta un nuevo registro de medidor.
     * @Valid activa las validaciones de Jakarta definidas en RegistroMedidorRequestDTO.
     */
    @PostMapping
    public ResponseEntity<RegistroMedidorResponseDTO> create(
            @Valid @RequestBody RegistroMedidorRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroService.create(dto));
    }

    /**
     * Lista paginada de registros. El frontend controla la página desde el cliente.
     *
     * @param page         Número de página (default: 0 = primera página)
     * @param size         Registros por página (default: 15)
     * @param tipoServicio Filtro opcional: 1=Luz, 2=Agua
     */
    @GetMapping
    public ResponseEntity<Page<RegistroMedidorResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Integer tipoServicio
    ) {
        return ResponseEntity.ok(registroService.findAll(tipoServicio, page, size));
    }

    /**
     * Endpoint de reporte por mes, año y tipo de servicio.
     * Retorna lista plana (sin paginar) para exportación a Excel.
     *
     * @param mes          Número de mes (1-12)
     * @param anio         Año de cuatro dígitos (ej. 2025)
     * @param tipoServicio 1=Luz, 2=Agua, 0 o ausente = Ambos
     */
    @GetMapping("/reporte")
    public ResponseEntity<List<RegistroMedidorResponseDTO>> getReporte(
            @RequestParam int mes,
            @RequestParam int anio,
            @RequestParam(required = false) Integer tipoServicio
    ) {
        Integer tipoFiltro = (tipoServicio == null || tipoServicio == 0) ? null : tipoServicio;
        return ResponseEntity.ok(registroService.findReporte(mes, anio, tipoFiltro));
    }
}
