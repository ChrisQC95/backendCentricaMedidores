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
     *   "fechaRegistro": "2025-05-26",  // opcional — default: hoy
     *   "tipoServicio": 1               // 1=Luz, 2=Agua
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
    public ResponseEntity<List<RegistroMedidorResponseDTO>> findAll(
            @RequestParam(required = false) Integer tipoServicio
    ) {
        return ResponseEntity.ok(registroService.findAll(tipoServicio));
    }

    /**
     * Endpoint de reporte por mes, año y tipo de servicio.
     * Devuelve las filas exactas para exportación a Excel.
     *
     * Ejemplos:
     *   GET /api/medidores/reporte?mes=5&anio=2025&tipoServicio=1  → solo Luz
     *   GET /api/medidores/reporte?mes=5&anio=2025&tipoServicio=2  → solo Agua
     *   GET /api/medidores/reporte?mes=5&anio=2025                 → Ambos (sin filtro)
     *   GET /api/medidores/reporte?mes=5&anio=2025&tipoServicio=0  → Ambos (sin filtro)
     *
     * Cada fila lleva su campo tipoServicio para que el frontend/Excel
     * muestre la unidad correcta (kWh o m³) sin mezclar totales.
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
        // 0 se usa como "Ambos" desde el frontend; lo normalizamos a null para el service
        Integer tipoFiltro = (tipoServicio == null || tipoServicio == 0) ? null : tipoServicio;
        return ResponseEntity.ok(registroService.findReporte(mes, anio, tipoFiltro));
    }
}
