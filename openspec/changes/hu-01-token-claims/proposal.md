# Proposal: Token firmado con expiración y claims mínimos

## Issue de referencia

- GitHub Issue [#11](https://github.com/user-bcp-student12/workshop-auth-backend/issues/11): "HU-01 - Token firmado con expiracion y claims minimos".
- Fuente de la HU: [HU-01.md](../../../HU-01.md).

## Why

`POST /auth/login` actualmente emite un token legacy sin firma: concatena username, role, expiración y un secreto embebido, y codifica el resultado con Base64. No es HMAC ni permite verificar la integridad del contenido. El cambio sustituye ese formato por un JWT compacto firmado con HS256 y los claims mínimos (`sub`, `client`, `iat`, `exp`). Además, `expiresIn` está fijo en `3600` y no se verifica contra la expiración real del token, y el secreto por defecto puede llegar a producción sin control.

## Baseline que debe preservarse

Referencia: [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md).

- Ruta y método: `POST /auth/login`, `application/json` de entrada y salida.
- Campos de request: `username` y `password` obligatorios; `client` opcional. Campos desconocidos se ignoran.
- Respuestas de error y sus códigos: 400 (`Request is null`, `Username is required`, `Password is required`), 401 (`Invalid user <username> or password`), 500 (forma `{"message", "exception"}`).
- Forma del cuerpo 200: `token`, `expiresIn`, `user` (`username`, `displayName`, `role`) y `debug`.
- `user.displayName` sigue con escape HTML.
- `expiresIn` sigue siendo un número de segundos.
- Ningún log debe contener tokens, secretos ni contraseñas.

## What Changes

### Dentro de alcance

- El token emitido en login incluye `sub`, `client`, `iat` y `exp`, y conserva `role` como claim adicional.
- Se emite un JWT compacto firmado con HS256 usando `AUTH_TOKEN_SECRET` configurable.
- `client` se valida contra una lista permitida; si se omite, se usa `angular-workshop` como valor por defecto aprobado.
- `expiresIn` coincide con `exp - iat`; la verificación de reloj usa una tolerancia aprobada de 5 segundos.
- La verificación interna rechaza firmas inválidas y tokens expirados. No se agrega un endpoint HTTP de verificación.
- El secreto por defecto queda restringido a laboratorio y documentado, o se elimina fuera de laboratorio.
- El login y los errores de persistencia no registran tokens, claves, contraseñas ni SQL con valores sensibles.
- Pruebas para login válido, credenciales inválidas, claims y expiración.

### Fuera de alcance

- Cambios en otros endpoints, incluido `GET /q/health`.
- Refresh tokens, revocación o listas de bloqueo.
- Cambios en el modelo de usuarios, esquema o consulta SQL de `LegacyDatabase`; solo se permite el ajuste de logging indicado.
- Migración de clientes existentes a un nuevo esquema de autenticación. El responsable confirmó que ningún consumidor externo depende de decodificar el token Base64 actual.

### Cambios de contrato propuestos

1. `client` permanece opcional. Si se omite, se usa `angular-workshop`; si se envía un valor no permitido, la respuesta es 400.
2. El valor de `token` cambia de Base64 legacy a JWT firmado, pero la forma externa del cuerpo 200 se mantiene.

## Capabilities

- Nueva: `auth-login`, definida en [specs/auth-login/spec.md](specs/auth-login/spec.md).

## Impact

- Afecta al flujo de `POST /auth/login` (`AuthResource`, `LoginRequest`), a `AuthTokenService`, a la configuración de `AUTH_TOKEN_SECRET`, al logging de errores de credenciales y a las pruebas de autenticación.
- No afecta a otros endpoints ni al modelo o esquema de usuarios.

## Preguntas abiertas

- ¿El campo `debug` del 200 se mantiene en producción? Este cambio lo conserva para no alterar el contrato existente.
- La lista de clientes fuera del perfil de laboratorio se configura por entorno; `angular-workshop` es el valor de laboratorio y el fallback aprobado.
