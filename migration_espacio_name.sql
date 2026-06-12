-- ============================================================
-- MIGRACIÓN BACKEND — Centrica Medidores
-- Ejecutar en Supabase → SQL Editor antes de reiniciar el backend
-- ============================================================

-- TAREA 1: Añadir columna espacio_name a la tabla infraestructura
-- 1 = Oficina, 2 = Almacén
-- Es nullable porque no todos los nodos son espacios concretos (edificios, pisos)
ALTER TABLE infraestructura
    ADD COLUMN IF NOT EXISTS espacio_name INTEGER;

COMMENT ON COLUMN infraestructura.espacio_name
    IS '1 = Oficina, 2 = Almacén. NULL para nodos no-espacio (UNIDAD, PISO, etc.)';

-- ============================================================
-- Verificación rápida (opcional — puedes copiar solo el ALTER)
-- ============================================================
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'infraestructura'
ORDER BY ordinal_position;
