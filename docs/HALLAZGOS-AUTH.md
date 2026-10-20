# Hallazgos candidatos - Auth Backend

Alcance: revision local de HU-01, el endpoint de login y sus pruebas en `workshop-auth-backend`. No incluye hallazgos que dependan de comparar con el frontend o la API de requerimientos. Todos los puntos requieren revision humana; no son issues aprobados.

### La implementacion del token no corresponde a la firma/configuracion descrita en HU-01

- Estado: CANDIDATO
- Evidencia: `HU-01.md:7` describe firma HMAC y lectura de `AUTH_TOKEN_SECRET`. `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:7,14-15` usa un secreto constante dentro de la clase y codifica el contenido con Base64; no se observa una operacion HMAC en el metodo de creacion.
- Impacto: Base64 no permite verificar integridad ni autenticidad. Ademas, configurar o rotar `AUTH_TOKEN_SECRET` no afecta el token producido por esta implementacion.
- Prioridad propuesta: P1 antes de cualquier uso fuera del laboratorio.
- Contrato potencialmente afectado: semantica del campo `token` en `POST /auth/login`; conservar el contrato del endpoint indicado en `REGLAS.md:4`.
- Como validarlo: comprobar mediante pruebas que el token no sea aceptado despues de alterar su contenido y que la configuracion de la clave controle la verificacion, sin exponer valores sensibles.

### El token no refleja los claims requeridos ni el `client` recibido

- Estado: CANDIDATO
- Evidencia: `HU-01.md:10` solicita `sub`, `client`, `iat` y `exp`. `src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6` declara `client`, pero `src/main/java/com/bcp/workshop/auth/AuthResource.java:39` invoca el creador del token solo con username y role. `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:12-15` serializa campos delimitados, no un conjunto de claims JWT.
- Impacto: la implementacion actual no demuestra que cumpla los claims exigidos y no utiliza ni valida `client` al crear el token.
- Prioridad propuesta: P1 para aceptar HU-01.
- Contrato potencialmente afectado: cuerpo de solicitud de `POST /auth/login` y contenido del campo `token`; mantener `token`, `expiresIn` y `user` en la respuesta, segun `HU-01.md:7` y `REGLAS.md:4`.
- Como validarlo: decodificar y verificar tokens de pruebas para comprobar cada claim requerido; enviar `client` valido e invalido y comprobar su validacion y reflejo en el token.

### `expiresIn` no se calcula a partir del vencimiento emitido

- Estado: CANDIDATO
- Evidencia: `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:13` calcula un vencimiento relativo a `Instant.now()`, mientras `src/main/java/com/bcp/workshop/auth/AuthResource.java:42` responde siempre con `expiresIn` igual a 3600. HU-01 requiere coherencia entre `expiresIn` y `exp - now` con tolerancia documentada (`HU-01.md:12,19`).
- Impacto: el valor de respuesta y el vencimiento del token se calculan por separado; la tolerancia y la coherencia exacta no quedan garantizadas ni documentadas por este flujo.
- Prioridad propuesta: P2 antes de aceptar HU-01.
- Contrato potencialmente afectado: relacion entre los campos `token` y `expiresIn` de la respuesta de `POST /auth/login`.
- Como validarlo: controlar el reloj en una prueba y comparar el `exp` emitido con `expiresIn`, usando la tolerancia que se acuerde y documente.

### La prueba de login no verifica los criterios de HU-01

- Estado: CANDIDATO
- Evidencia: `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:12-22` contiene una prueba de login exitoso que comprueba el usuario y que `token` no sea nulo. No verifica claims, `expiresIn`, credenciales invalidas ni expiracion, aunque HU-01 los incluye entre sus criterios (`HU-01.md:18-21`).
- Impacto: cambios en claims, expiracion o manejo de errores podrian incumplir HU-01 sin que esta prueba de backend lo detecte.
- Prioridad propuesta: P2 antes de cerrar HU-01.
- Contrato potencialmente afectado: respuesta y errores de `POST /auth/login`; no ampliar ni cambiar el contrato protegido por `REGLAS.md:4` sin aprobacion.
- Como validarlo: agregar casos automatizados para login valido, credenciales invalidas, claims requeridos, coherencia de expiracion y token expirado, y comprobar sus aserciones contra el contrato aprobado.

## Revision humana

Los cuatro hallazgos permanecen como `CANDIDATO`. Confirmar prioridad y alcance antes de crear issues o implementar cambios.