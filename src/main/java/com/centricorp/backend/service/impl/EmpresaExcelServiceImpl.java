package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.CargaMasivaResponseDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.EmpresaExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaExcelServiceImpl implements EmpresaExcelService {

    private final EmpresaRepository empresaRepository;

    @Override
    public ByteArrayInputStream generarPlantillaExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Empresas");

            // Estilo Estricto de la Cabecera (Fondo negro, texto blanco, negrita, centrado)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            // Crear fila 0 (Cabecera)
            Row headerRow = sheet.createRow(0);

            Cell cellRuc = headerRow.createCell(0);
            cellRuc.setCellValue("Ruc");
            cellRuc.setCellStyle(headerStyle);

            Cell cellRazonSocial = headerRow.createCell(1);
            cellRazonSocial.setCellValue("Razon Social");
            cellRazonSocial.setCellStyle(headerStyle);

            // Autoajustar columnas
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // Congelar la primera fila
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            log.error("Error al generar la plantilla de Excel", e);
            throw new RuntimeException("No se pudo generar la plantilla Excel");
        }
    }

    @Override
    @Transactional
    public CargaMasivaResponseDTO procesarCargaMasiva(MultipartFile file) {
        List<String> errores = new ArrayList<>();
        List<Empresa> empresasBatch = new ArrayList<>();
        Set<String> rucsProcesadosEnArchivo = new HashSet<>();
        
        int procesados = 0;
        int exitosos = 0;
        String tenantId = TenantContext.getCurrentTenant();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Iterar sobre las filas
            for (Row row : sheet) {
                // Saltar la cabecera (fila 0)
                if (row.getRowNum() == 0) {
                    continue;
                }

                // Evitar NullPointerException si la fila está completamente vacía
                if (row.getCell(0) == null && row.getCell(1) == null) {
                    continue;
                }

                procesados++;

                String ruc = getCellValueAsString(row.getCell(0));
                String razonSocial = getCellValueAsString(row.getCell(1));

                // Validación de RUC
                if (ruc == null || ruc.trim().isEmpty()) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": RUC vacío");
                    continue;
                }
                ruc = ruc.trim();
                
                if (ruc.length() != 11 || !ruc.matches("\\d+")) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": RUC inválido (Debe contener exactamente 11 dígitos numéricos)");
                    continue;
                }

                // Validación de Razón Social
                if (razonSocial == null || razonSocial.trim().isEmpty()) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": Razón Social vacía");
                    continue;
                }
                razonSocial = razonSocial.trim();

                // Validación de Duplicidad en el mismo Excel
                if (!rucsProcesadosEnArchivo.add(ruc)) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": RUC " + ruc + " duplicado dentro del mismo archivo");
                    continue;
                }

                // Validación de Duplicidad en Base de Datos
                if (empresaRepository.existsByRucAndTenantId(ruc, tenantId)) {
                    errores.add("Fila " + (row.getRowNum() + 1) + ": El RUC " + ruc + " ya se encuentra registrado en el sistema");
                    continue; // Se salta para no estallar el lote entero
                }

                // Si todo es válido, lo agregamos al Batch
                Empresa empresa = Empresa.builder()
                        .ruc(ruc)
                        .razonSocial(razonSocial)
                        .tenantId(tenantId)
                        .build();

                empresasBatch.add(empresa);
                exitosos++;
            }

            // Inserción en Batch
            if (!empresasBatch.isEmpty()) {
                empresaRepository.saveAll(empresasBatch);
            }

        } catch (IOException e) {
            log.error("Error al leer el archivo Excel", e);
            throw new RuntimeException("Error al procesar el archivo: Verifique que sea un formato .xlsx válido");
        }

        return CargaMasivaResponseDTO.builder()
                .procesados(procesados)
                .exitosos(exitosos)
                .errores(errores.size())
                .detalleErrores(errores)
                .build();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (type == CellType.NUMERIC) {
            return String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue());
        }
        return null;
    }
}
