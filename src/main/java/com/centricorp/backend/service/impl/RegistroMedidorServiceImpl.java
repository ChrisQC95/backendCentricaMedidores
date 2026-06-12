package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;
import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.entity.RegistroMedidor;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.service.RegistroMedidorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroMedidorServiceImpl implements RegistroMedidorService {

    private final RegistroMedidorRepository registroRepo;
    private final InfraestructuraRepository infraRepo;

    @Override
    @Transactional
    public RegistroMedidorResponseDTO create(RegistroMedidorRequestDTO dto) {
        if (dto.getTipoServicio() != null && dto.getTipoServicio() != 1 && dto.getTipoServicio() != 2) {
            throw new IllegalArgumentException("El tipo de servicio debe ser 1 (Luz) o 2 (Agua).");
        }

        // Validar que el nodo de infraestructura exista → 404 si no
        Infraestructura infra = infraRepo.findById(dto.getInfraestructuraId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Infraestructura", dto.getInfraestructuraId()));

        RegistroMedidor registro = RegistroMedidor.builder()
                .infraestructura(infra)
                .fotoUrl(dto.getFotoUrl())
                .voltaje(dto.getVoltaje())
                .observacion(dto.getObservacion())
                // Si el cliente no envía fecha, usamos la fecha actual como el DEFAULT de BD
                .fechaRegistro(dto.getFechaRegistro() != null
                        ? dto.getFechaRegistro()
                        : LocalDate.now())
                .tipoServicio(dto.getTipoServicio() != null ? dto.getTipoServicio() : 1) // Default a 1 (Luz)
                // consumo NO se asigna — insertable=false garantiza que no se manda en el INSERT
                .build();

        RegistroMedidor saved = registroRepo.save(registro);

        // Re-fetch para obtener el consumo calculado por el trigger de BD
        // El trigger corre AFTER INSERT, así que refreshamos con findById
        return toDTO(registroRepo.findById(saved.getId())
                .orElse(saved));
    }

    @Override
    public List<RegistroMedidorResponseDTO> findAll(Integer tipoServicio) {
        List<RegistroMedidor> registros = (tipoServicio != null) 
                ? registroRepo.findByTipoServicio(tipoServicio) 
                : registroRepo.findAll();
        return registros.stream().map(this::toDTO).toList();
    }

    @Override
    public List<RegistroMedidorResponseDTO> findReporte(int mes, int anio, Integer tipoServicio) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12.");
        }
        // Si tipoServicio no es null, debe ser 1 o 2
        if (tipoServicio != null && tipoServicio != 1 && tipoServicio != 2) {
            throw new IllegalArgumentException("El tipo de servicio debe ser 1 (Luz) o 2 (Agua), o no indicarse para obtener ambos.");
        }
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fin    = ym.atEndOfMonth();

        // null = Ambos tipos — devuelve todos los registros del periodo con su tipoServicio
        // El frontend separa la unidad (kWh / m³) por fila usando row.tipoServicio
        List<com.centricorp.backend.entity.RegistroMedidor> registros = (tipoServicio == null)
                ? registroRepo.findByFechaRegistroBetween(inicio, fin)
                : registroRepo.findByFechaRegistroBetweenAndTipoServicio(inicio, fin, tipoServicio);

        return registros.stream().map(this::toDTO).toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RegistroMedidorResponseDTO toDTO(RegistroMedidor r) {
        Infraestructura infra = r.getInfraestructura();
        return RegistroMedidorResponseDTO.builder()
                .id(r.getId())
                .infraestructuraId(infra.getId())
                .infraestructuraNombre(infra.getNombre())
                .infraestructuraTipo(infra.getTipo() != null
                        ? infra.getTipo().name() : null)
                .empresaRuc(infra.getEmpresa().getRuc())
                .empresaRazonSocial(infra.getEmpresa().getRazonSocial())
                .fotoUrl(r.getFotoUrl())
                .voltaje(r.getVoltaje())
                .consumo(r.getConsumo())
                .fechaRegistro(r.getFechaRegistro())
                .observacion(r.getObservacion())
                .tipoServicio(r.getTipoServicio())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
