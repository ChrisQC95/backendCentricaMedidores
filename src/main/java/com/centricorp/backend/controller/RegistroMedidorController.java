package com.centricorp.backend.controller;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;
import com.centricorp.backend.service.RegistroMedidorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de Registro de Medidores.
 *
 * POST /api/medidores                         → insertar nuevo registro
 * GET  /api/medidores                         → listar todos los registros
 * GET  /api/medidores/reporte?mes=5&anio=2025 → registros filtrados por mes y año
 *                                               (para exportación a Excel)
 */
@RestController
@RequestMapping("/api/medidores")
@RequiredArgsConstructor
public class RegistroMedidorController {

    private final RegistroMedidorService registroService;

    /**
     * Inserta un nuevo registro de medidor.
     * El campo 'consumo' es calculado por trigger en PostgreSQL —
     * nunca se envía en el body, se devuelve en la respuesta (puede ser null
     * si es el primer registro de ese medidor).
     *
     * Body esperado:
     * {
     *   "infraestructuraId": 5,
     *   "voltaje": 220.50,
     *   "fotoUrl": "https://cdn.example.com/foto123.jpg",
     *   "observacion": "Medidor en buen estado",
     *   "fechaRegistro": "2025-05-26"   // opcional — default: hoy
     * }
     */
    @PostMapping
    public ResponseEntity<RegistroMedidorResponseDTO> create(
            @RequestBody RegistroMedidorRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<RegistroMedidorResponseDTO>> findAll() {
        return ResponseEntity.ok(registroService.findAll());
    }

    /**
     * Endpoint de reporte por mes y año.
     * Devuelve las filas exactas para exportación a Excel.
     *
     * Ejemplo: GET /api/medidores/reporte?mes=5&anio=2025
     *
     * @param mes  Número de mes (1-12)
     * @param anio Año de cuatro dígitos (ej. 2025)
     */
    @GetMapping("/reporte")
    public ResponseEntity<List<RegistroMedidorResponseDTO>> getReporte(
            @RequestParam int mes,
            @RequestParam int anio
    ) {
        return ResponseEntity.ok(registroService.findReporte(mes, anio));
    }
}
