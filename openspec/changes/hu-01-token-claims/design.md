## Context

`POST /auth/login` es el único endpoint de autenticación. Su flujo actual es:

1. `AuthResource.login` valida `request`, `username` y `password`.
2. `LegacyDatabase.findByCredentials` consulta `legacy_users` en SQLite.
3. `LegacyTokenUtil.create(username, role)` construye el token legacy.
4. La respuesta 200 incluye `token`, `expiresIn` (fijo en `3600`), `user` y `debug`.

Hallazgos verificados en el código (ver [docs/HALLAZGOS-AUTH.md](../../../docs/HALLAZGOS-AUTH.md)):

- El token actual no está firmado. `LegacyTokenUtil` concatena `username|role|exp|SECRET` y lo codifica en Base64; no hay HMAC y `AUTH_TOKEN_SECRET` no se lee.
- El secreto `WORKSHOP_SECRET_2026` está escrito en el código fuente.
- `client` se declara en `LoginRequest` pero no se usa al emitir el token.
- `expiresIn` y el `exp` legacy se calculan por separado.
- `AuthResourceTest` ya caracteriza la respuesta, validaciones, credenciales inválidas, cliente ausente y escape HTML; las pruebas de claims y firma siguen pendientes.

La propuesta describe el token objetivo firmado con HS256. El formato actual es Base64 sin firma; no se confunden ambos estados.

## Goals / Non-Goals

### Goals

- Emitir JWT compacto firmado con HS256 y los claims `sub`, `client`, `iat`, `exp` y `role`.
- Hacer que `expiresIn` sea `exp - iat`, usando `TOKEN_TTL_SECONDS = 3600`.
- Leer `AUTH_TOKEN_SECRET` por configuración y eliminar el secreto embebido.
- Validar `client` contra la lista configurada; si falta, usar `angular-workshop`.
- Verificar firma y expiración dentro de `AuthTokenService`, sin endpoint HTTP nuevo.
- Evitar que tokens, clave, contraseñas o SQL con credenciales aparezcan en logs.
- Mantener la cobertura del código modificado en 85% o superior.

### Non-Goals

- No cambiar el esquema de `legacy_users` ni la consulta SQL. Se permite únicamente quitar el logging de excepciones sensibles de `LegacyDatabase`.
- No añadir refresh tokens, revocación ni listas de bloqueo.
- No cambiar `GET /q/health` ni otros endpoints.
- No migrar clientes externos; se confirmó que ninguno depende de decodificar el token Base64 actual.
- No agregar un endpoint HTTP de verificación.

## Protected contracts

Todo lo siguiente se mantiene idéntico salvo el valor opaco de `token` y el comportamiento aprobado de `client`:

- Ruta y método: `POST /auth/login`, `application/json` de entrada y salida.
- `username` y `password` son obligatorios; `client` continúa opcional y usa `angular-workshop` cuando se omite.
- Se conservan los códigos y mensajes 400/401/500 documentados en `docs/ENDPOINTS.md`.
- La forma del 200 continúa con `token`, `expiresIn`, `user` (`username`, `displayName`, `role`) y `debug`.
- `user.displayName` continúa escapado con `StringEscapeUtils.escapeHtml4`.
- `expiresIn` continúa siendo un entero en segundos.

## Components affected

| Componente | Cambio |
|---|---|
| `AuthResource` | Delegar emisión al servicio de token y mantener la forma de respuesta. |
| `LegacyTokenUtil` | Retirar tras la verificación de caracterización y el cambio al nuevo servicio. |
| `LoginRequest` | Mantener estructura; `client` ya existe. |
| `AuthTokenService` | Emitir/verificar internamente JWT HS256 con `sub`, `client`, `iat`, `exp`, `role`. |
| Configuración | Leer `AUTH_TOKEN_SECRET`; valor de laboratorio en `%dev`/`%test`, sin default en producción. |
| `LegacyDatabase` | Quitar impresión de excepciones que pueda filtrar SQL/credenciales, sin cambiar query ni esquema. |
| Pruebas y `docs/ENDPOINTS.md` | Cubrir los criterios aprobados y documentar el token opaco y `expiresIn`. |

## Decisions

### D1. Formato del token (aprobada)

Se implementa JWT compacto HS256 con `javax.crypto.Mac`, sin dependencia JWT adicional. Base64 legacy no se considera firma.

### D2. Clave de firma

- `AUTH_TOKEN_SECRET` se lee de configuración/variable de entorno.
- `%dev` y `%test` usan valor de laboratorio documentado, no real.
- Producción no tiene default; el arranque falla si falta la clave.
- Se elimina `WORKSHOP_SECRET_2026` del código.

### D3. Duración y `expiresIn` (tolerancia aprobada)

- Una constante `TOKEN_TTL_SECONDS = 3600`.
- `iat = now`, `exp = iat + TOKEN_TTL_SECONDS`, `expiresIn = TOKEN_TTL_SECONDS`.
- `exp - iat == expiresIn`; la prueba de `exp - now` usa tolerancia de 5 segundos.

### D4. Validación de `client` (aprobada)

- Lista permitida configurable; `angular-workshop` es el valor de laboratorio.
- `client` ausente usa `angular-workshop`, preservando su opcionalidad.
- `client` presente pero no permitido responde 400 sin token.

### D5. Rol en el token (aprobada)

Se conserva `role` como claim adicional para no perder información del token legacy.

### D6. Logs (alcance aprobado)

`LegacyDatabase.findByCredentials` usa `error.printStackTrace()`, que puede imprimir el SQL concatenado con `password`. Se elimina ese volcado o se sustituye por mensaje genérico sin excepción, SQL ni credenciales. No se modifica la consulta ni el esquema; se prueba que el log no exponga valores sensibles.

## Estrategia incremental

1. Caracterización actual completada: respuesta 200, validaciones 400, credenciales inválidas 401, cliente ausente y escape HTML; suite 7/7 aprobada con JDK 21.
2. Implementar `AuthTokenService` HS256 con `sub`, `client`, `iat`, `exp`, `role` y clave configurada; conectar `AuthResource` sin cambiar el contrato.
3. Probar claims y firma, incluido rechazo de tokens alterados.
4. Alinear `expiresIn` con `exp - iat` y probar la tolerancia de reloj aprobada.
5. Verificar expiración internamente; no agregar endpoint HTTP.
6. Configurar `AUTH_TOKEN_SECRET` por perfil y probar que producción falla si falta.
7. Validar clientes permitidos y default ausente.
8. Evitar y probar logs sensibles, incluido el error de persistencia sin tocar SQL.
9. Retirar `LegacyTokenUtil` y documentar el token opaco y configuración.

Cada paso debe dejar la suite en verde y ser revisable. La consulta SQL concatenada es un hallazgo secundario fuera de alcance y no se corregirá aquí.

## Rollback

- Cada paso es un cambio revisable; revertir la conexión al servicio restaura el flujo anterior.
- `LegacyTokenUtil` se retira solo al final, después de validar el nuevo servicio.
- Los tokens HS256 dejan de ser verificables tras revertir el código; no existe migración necesaria de consumidores externos confirmados.
