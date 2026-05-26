package com.centricorp.backend.entity;

/**
 * Mapea el tipo ENUM PostgreSQL: tipo_nivel
 * CREATE TYPE tipo_nivel AS ENUM ('UNIDAD','BLOQUE','PISO','ENST','ESPACIO_COMUN');
 */
public enum TipoNivel {
    UNIDAD,
    BLOQUE,
    PISO,
    ENST,
    ESPACIO_COMUN
}
