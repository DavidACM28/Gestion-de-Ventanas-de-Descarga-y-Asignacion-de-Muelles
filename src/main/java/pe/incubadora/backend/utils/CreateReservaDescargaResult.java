package pe.incubadora.backend.utils;

public enum CreateReservaDescargaResult {
    MUELLE_NOT_FOUND,
    CAMION_NOT_FOUND,
    TIPO_CARGA_INVALIDA,
    PESO_EXCEDE_MUELLE,
    FECHA_INVALIDA,
    HORA_INVALIDA,
    FECHA_PASADA,
    DURACION_INVALIDA,
    CIERRE_CONFLICT,
    CREATED
}
