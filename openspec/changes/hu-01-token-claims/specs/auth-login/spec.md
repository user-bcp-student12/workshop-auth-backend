## ADDED Requirements
### Requirement: El token contiene claims mínimos y firma HS256

El token emitido por `POST /auth/login` con credenciales válidas MUST ser un JWT compacto firmado con HS256 y contener `sub`, `client`, `iat` y `exp`. MUST conservar `role` como claim adicional.

#### Scenario: Login válido emite token con claims completos

- GIVEN credenciales válidas para `username` y `password`
- AND una solicitud con `client` válido
- WHEN se llama a `POST /auth/login`
- THEN la respuesta es 200
- AND el `token` verificado contiene `sub` igual al `username` autenticado
- AND contiene `client` igual al valor enviado en la solicitud
- AND contiene `role` igual al rol del usuario autenticado
- AND contiene `iat` y `exp` como timestamps numéricos

#### Scenario: Token alterado no verifica

- GIVEN un token emitido por `AuthTokenService`
- WHEN se altera el contenido firmado
- THEN la verificación interna falla

### Requirement: `expiresIn` es coherente con `exp`

El campo `expiresIn` de la respuesta 200 MUST coincidir con `exp - iat`, y `exp - now` MUST quedar dentro de una tolerancia de 5 segundos.

#### Scenario: expiresIn coincide con la expiración del token

- GIVEN un login válido
- WHEN se decodifica el `token` de la respuesta
- THEN `exp - iat` coincide con `expiresIn`
- AND `exp - now` está dentro de la tolerancia documentada de 5 segundos

### Requirement: Validación de `client`

El servicio MUST validar el valor de `client` antes de emitir el token.

#### Scenario: client no permitido

- GIVEN credenciales válidas
- AND un `client` que no está en la lista permitida
- WHEN se llama a `POST /auth/login`
- THEN la respuesta es 400
- AND no se emite ningún token

#### Scenario: client ausente

- GIVEN credenciales válidas
- AND una solicitud sin el campo `client`
- WHEN se llama a `POST /auth/login`
- THEN se usa `angular-workshop` como valor por defecto
- AND el token contiene `client` igual a `angular-workshop`

### Requirement: Contrato de login preservado

El login MUST mantener el contrato definido en [docs/ENDPOINTS.md](../../../../../docs/ENDPOINTS.md): rutas, campos de request, códigos de error y forma del cuerpo de respuesta.

#### Scenario: Credenciales inválidas

- GIVEN un `username` existente y una `password` incorrecta
- WHEN se llama a `POST /auth/login`
- THEN la respuesta es 401
- AND el cuerpo es `{"message": "Invalid user <username> or password"}`

#### Scenario: Campo obligatorio vacío

- GIVEN una solicitud con `username` vacío o formado solo por espacios
- WHEN se llama a `POST /auth/login`
- THEN la respuesta es 400
- AND el cuerpo es `{"message": "Username is required"}`

#### Scenario: Forma de la respuesta 200

- GIVEN credenciales válidas
- WHEN se llama a `POST /auth/login`
- THEN el cuerpo contiene `token`, `expiresIn`, `user` y `debug`
- AND `user.displayName` está escapado en HTML

### Requirement: Token expirado se rechaza

El método interno de verificación de `AuthTokenService` MUST rechazar un token cuyo `exp` ya pasó. No se agrega un endpoint HTTP de verificación.

#### Scenario: Token expirado

- GIVEN un token emitido con `exp` en el pasado
- WHEN se invoca la verificación interna
- THEN la verificación falla

### Requirement: Secretos, tokens y SQL sensible no aparecen en logs

Los logs de login y los errores de persistencia MUST NOT contener el token, `AUTH_TOKEN_SECRET`, la contraseña ni SQL con valores de credenciales.

#### Scenario: Login y error de persistencia no registran valores sensibles

- GIVEN un login válido o un error de consulta de credenciales
- WHEN se revisan los logs generados
- THEN no aparecen el `token`, el secreto, la contraseña ni SQL con valores de credenciales

### Requirement: Secreto por defecto restringido

El secreto por defecto de `AUTH_TOKEN_SECRET` MUST estar documentado como exclusivo de laboratorio o eliminado fuera de laboratorio.

#### Scenario: Configuración documentada

- GIVEN el repositorio
- WHEN se consulta la documentación de configuración
- THEN `AUTH_TOKEN_SECRET` aparece documentado con su uso permitido

### Requirement: Decisión de formato del token documentada

La decisión aprobada de emitir JWT compacto HS256 con `javax.crypto.Mac` MUST quedar registrada en la documentación del cambio.

#### Scenario: Decisión registrada

- GIVEN el cambio completado
- WHEN se revisa la documentación del cambio
- THEN existe una decisión explícita de usar HS256 y no añadir una librería JWT
