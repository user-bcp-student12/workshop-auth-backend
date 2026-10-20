# Summary

Agregar una confirmación previa al envío de transferencias, con un resumen y validación de datos, para reducir en un 20% las quejas por transferencias duplicadas durante un mes.

# Scope

- **Dentro del alcance:** confirmación antes del envío, mensaje resumen y validación de datos.
- **Fuera del alcance:** cambios de infraestructura y rediseño completo de la interfaz.

# Supuestos

- Se utilizarán el frontend web y la API REST existentes.
- No se asumen reglas de validación, campos del resumen ni mecanismos específicos para medir las quejas; deben definirse.

# Decisiones técnicas por tomar (pendientes o resueltas)

- **Resueltas:** usar la API REST existente y el registro estandarizado.
- **Pendientes:** definir los datos que mostrará el resumen, las reglas de validación del monto y qué servicio gestiona cada validación.
- **Pendientes:** acordar cómo medir las quejas por transferencias duplicadas y qué periodo servirá de comparación.

# Riesgos y mitigaciones

- **Exposición de datos sensibles en logs:** revisar los datos registrados y comprobar que la información sensible se excluya o se proteja según el estándar existente.
- **Errores al validar montos:** acordar las reglas con el servicio responsable y probar los casos válidos y no válidos definidos.

# Criterios de aceptación (medibles)

- En el 100% de los casos de prueba del flujo, la confirmación se muestra antes de invocar la API de transferencias.
- En el 100% de los casos de prueba, no se invoca la API antes de que el usuario confirme.
- Para todos los casos de monto no válido definidos, la validación impide el envío a la API.
- La métrica acordada de quejas por transferencias duplicadas disminuye al menos un 20% durante el primer mes, comparada con el periodo de referencia definido.

# Plan de validación

- **Pruebas unitarias:** comprobar que el envío depende de la confirmación y que las reglas acordadas de validación se aplican.
- **Pruebas funcionales:** verificar que se presenta el resumen antes del envío y que la confirmación permite continuar el flujo.
- **Seguridad (comprobaciones mínimas):** revisar que los logs no expongan datos sensibles y que los errores de validación no revelen información sensible.
- **Evidencia requerida (qué adjuntar):** resultados de pruebas, evidencia de revisión de logs con datos protegidos y fuente de la métrica antes y después del periodo.

# Tareas sugeridas por rol

- **Chapter/PO:** definir la métrica y el periodo de referencia; acordar el contenido del resumen; confirmar las reglas de validación con las áreas responsables.
- **Tech Lead:** definir el flujo entre frontend y servicios; aclarar las decisiones técnicas pendientes; revisar dependencias y el estándar de logging.
- **Backend:** verificar el contrato de la API REST; confirmar dónde se aplican las reglas de validación del monto; comprobar que los logs sigan el estándar sin exponer datos sensibles.
- **Frontend:** implementar la confirmación previa; presentar el resumen acordado; conectar la confirmación con la API existente.
- **QA:** probar que no haya invocación antes de confirmar; probar los casos de validación acordados; recopilar resultados y evidencia funcional.
- **Cyber:** revisar el tratamiento de datos en logs; comprobar que los errores no expongan información sensible; registrar hallazgos y mitigaciones.

# Preguntas abiertas

- ¿Cómo se define y de dónde se obtiene la métrica de quejas por transferencias duplicadas?
- ¿Qué periodo se usará como referencia para medir la reducción del 20%?
- ¿Qué campos debe mostrar el resumen de la transferencia?
- ¿Cuáles son las reglas y los mensajes esperados para montos no válidos?
- ¿Qué comportamiento debe tener el flujo si el usuario no confirma?
- ¿La confirmación debe prevenir también envíos repetidos por doble clic o reintentos?