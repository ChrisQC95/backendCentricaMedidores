package com.centricorp.backend.service;

import com.centricorp.backend.dto.EmpresaDTO;

import java.util.List;

public interface EmpresaService {
    List<EmpresaDTO> findAll();
    EmpresaDTO findById(String ruc);
    EmpresaDTO create(EmpresaDTO dto);
    EmpresaDTO update(String ruc, EmpresaDTO dto);
    void delete(String ruc);
}
