package com.centricorp.backend.service;

import com.centricorp.backend.dto.CargaMasivaResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

public interface EmpresaExcelService {
    ByteArrayInputStream generarPlantillaExcel();
    CargaMasivaResponseDTO procesarCargaMasiva(MultipartFile file);
}
