package pe.incubadora.backend.utils;

public enum RegisterUsuarioResult {
    CREATED,
    CAMION_CONFLICT,
    ROL_NOT_FOUND,
    CAMION_NOT_FOUND,
    EMPRESA_NOT_FOUND,
    CAMION_NOT_MATCH,
    ROL_TRANSPORTISTA_CONFLICT,
    ROL_ADMINISTRATIVO_CONFLICT
}
