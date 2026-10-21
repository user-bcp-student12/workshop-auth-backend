## 0. Decisiones previas (bloquean las tareas marcadas)

- [x] 0.1 Confirmar D1: JWT compacto HS256 con `javax.crypto.Mac`, sin dependencia JWT adicional.
- [x] 0.2 Confirmar D4: `client` ausente usa `angular-workshop`; los clientes no permitidos responden 400.
- [x] 0.3 Confirmar D5: conservar `role` como claim adicional.
- [x] 0.4 Confirmar que ningún cliente externo depende de decodificar el token Base64 actual.
- [x] 0.5 Corregir la premisa factual del estado actual en [proposal.md](proposal.md); la spec describe el comportamiento objetivo.

## 1. Pruebas de caracterización (antes de cualquier cambio funcional)

- [x] 1.1 Añadir prueba del 200: campos `token`, `expiresIn` (entero `3600`), `user` (`username`, `displayName`, `role`) y `debug` con valor `authenticated-with-legacy-flow`.
- [x] 1.2 Añadir prueba del 400 para `username` vacío o solo con espacios, con mensaje exacto `Username is required`.
- [x] 1.3 Añadir prueba del 400 para `password` vacía o solo con espacios, con mensaje exacto `Password is required`.
- [x] 1.4 Añadir prueba del 400 para request nulo con mensaje `Request is null`, si la petición con body vacío lo reproduce; si no, documentar el resultado observado.
- [x] 1.5 Añadir prueba del 401 con mensaje exacto `Invalid user <username> or password`.
- [x] 1.6 Añadir prueba de escape HTML de `user.displayName` usando un doble aislado de `LegacyDatabase`, sin modificar la base de datos real.
- [x] 1.7 Ejecutar la suite y confirmar que todas las pruebas de caracterización pasan sobre el código actual.

## 2. Servicio de token HS256

- [x] 2.1 Definir la constante `TOKEN_TTL_SECONDS = 3600` y el nombre del algoritmo HS256 como constantes con nombre.
- [x] 2.2 Crear `AuthTokenService` que emita un JWT compacto HS256 con `sub`, `iat` y `exp`, usando la clave recibida por configuración.
- [x] 2.3 Añadir `client` y `role` al token emitido.
- [x] 2.4 Conectar `AuthResource` al servicio sin cambiar la forma de la respuesta; las pruebas de la sección 1 deben seguir en verde.
- [x] 2.5 Añadir prueba de verificación de firma: un token alterado en cualquier parte es rechazado.
- [x] 2.6 Usar comparación de firma en tiempo constante.

## 3. Claims

- [x] 3.1 Añadir prueba que decodifica el token y comprueba `sub` igual al `username` autenticado.
- [x] 3.2 Añadir prueba que comprueba `iat` y `exp` como números y `client` igual al valor enviado.

## 4. Coherencia de `expiresIn`

- [x] 4.1 Hacer que `expiresIn` use `TOKEN_TTL_SECONDS` y que `exp = iat + TOKEN_TTL_SECONDS`.
- [x] 4.2 Añadir prueba de que `exp - iat` es igual a `expiresIn`.
- [x] 4.3 Inyectar `Clock` en el servicio y probar que `exp - now` está dentro de la tolerancia aprobada de 5 segundos.

## 5. Expiración

- [x] 5.1 Añadir verificación interna de firma y expiración en `AuthTokenService`, sin endpoint HTTP.
- [x] 5.2 Añadir prueba con un token cuyo `exp` está en el pasado: la verificación debe fallar.

## 6. Clave de firma por configuración

- [x] 6.1 Leer `AUTH_TOKEN_SECRET` desde configuración en `application.properties`.
- [x] 6.2 Definir valor de laboratorio solo para los perfiles `%dev` y `%test`, sin secreto real.
- [x] 6.3 Hacer que el perfil de producción no tenga valor por defecto.
- [ ] 6.4 Eliminar el secreto `WORKSHOP_SECRET_2026` del código fuente (la eliminación de la clase va en la sección 9).
- [ ] 6.5 Verificar que el arranque en producción falla si falta `AUTH_TOKEN_SECRET`, y documentar cómo comprobarlo.

## 7. Validación de `client`

- [ ] 7.1 Definir la lista de clientes permitidos en configuración, con `angular-workshop` como valor de laboratorio.
- [ ] 7.2 Añadir prueba: `client` no permitido responde 400 y no emite token.
- [ ] 7.3 Implementar y probar que `client` ausente usa `angular-workshop` como valor por defecto.

## 8. Logs sin valores sensibles

- [ ] 8.1 Añadir prueba o revisión que confirme que el login no registra token, clave ni contraseña en la salida.
- [ ] 8.2 Quitar el volcado de excepciones de `LegacyDatabase` que puede imprimir SQL/contraseña, sin modificar la consulta ni el esquema; usar logging genérico no sensible y probarlo.

## 9. Retirar la implementación legacy

- [ ] 9.1 Eliminar `LegacyTokenUtil.java` solo después de que las secciones 2 a 8 estén en verde.
- [ ] 9.2 Buscar en el repositorio referencias restantes a `LegacyTokenUtil` y a `WORKSHOP_SECRET_2026` y eliminarlas.

## 10. Documentación

- [ ] 10.1 Actualizar en [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md) la descripción de `token`, `expiresIn` y las notas de seguridad.
- [ ] 10.2 Documentar `AUTH_TOKEN_SECRET`: uso permitido, valor de laboratorio y obligatoriedad en producción.
- [ ] 10.3 Registrar la decisión D1 (HMAC frente a JWT) en la documentación del cambio.

## 11. Validación final

- [ ] 11.1 Ejecutar la suite completa de pruebas y confirmar que pasa.
- [ ] 11.2 Confirmar cobertura de 85% o superior en el código modificado (REGLAS, punto 7).
- [ ] 11.3 Revisar que CodeQL y Dependency Review no tienen alertas nuevas y que no se han silenciado (REGLAS, punto 4).
- [ ] 11.4 Ejecutar `openspec validate hu-01-token-claims --strict`.

## Fuera de alcance (no incluido en estas tareas)

- Corregir la concatenación SQL de `LegacyDatabase.findByCredentials`. Se registra como hallazgo aparte.
- Eliminar la salida de detalles internos en la respuesta 500.
- Refresh tokens, revocación, otros endpoints o cambios en el modelo de usuarios.
