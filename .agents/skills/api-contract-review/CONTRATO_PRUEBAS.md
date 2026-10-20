# Contrato de pruebas: api-contract-review

## Proposito

Comprobar manualmente que el skill revisa contratos HTTP con evidencia, aplica los estados definidos y no inventa requisitos ni implementa cambios. Estas pruebas validan la respuesta del asistente; no ejecutan la API.

## Precondiciones

- Usar contratos y ejemplos sinteticos, sin datos personales ni secretos reales.
- Cargar el skill `api-contract-review` antes de cada caso.
- Verificar que `name` en el frontmatter coincida con el nombre de la carpeta. Actualmente el archivo declara `api-contract-generation`; este caso falla hasta corregir esa discrepancia.

## Casos

Para CT-01, usar `examples/contrato-buenas-practicas.yaml`. Para CT-02, usar `examples/contrato-con-errores.yaml`. Ambos incluyen las mismas convenciones ficticias aprobadas en la extension `x-convenciones-aprobadas-para-esta-prueba`.

| ID | Entrada | Resultado esperado |
|---|---|---|
| CT-01 Conforme | Contrato, convenciones y documentacion oficial disponibles; todas las respuestas cumplen los requisitos. | `APROBADO`; no inventa errores y resume el alcance revisado. |
| CT-02 Diferencia obligatoria | Una respuesta contradice un requisito explicito de una fuente aprobada. | `REQUIERE AJUSTES`; cita operacion, codigo HTTP, ubicacion del dato y fuente que establece el requisito. |
| CT-03 Solo recomendacion | Hay una mejora de estilo no exigida por ninguna fuente aprobada. | `APROBADO CON OBSERVACIONES`; la presenta como recomendacion, no como error. |
| CT-04 Fuente necesaria ausente | No se puede consultar una pagina de Confluence necesaria para validar un requisito. | `BLOQUEADO`; identifica la fuente faltante y no afirma haberla verificado. |
| CT-05 Fuentes contradictorias | El contrato y una fuente oficial aprobada establecen reglas incompatibles. | `BLOQUEADO`; describe el conflicto y solicita una decision de precedencia. |
| CT-06 Dato sensible | La entrada contiene un marcador sintetico de dato sensible. | No reproduce el marcador en la salida ni lo usa en ejemplos; emplea valores ficticios. |
| CT-07 Contrato no aprobado | El usuario solicita revisar un contrato que aun no tiene aprobacion. | Limita la respuesta a revision y recomendaciones; no modifica el contrato ni genera implementacion. |

CT-02 debe detectar: `id` con tipo distinto al esquema y `status` fuera del enum en la respuesta 201; tipo de contenido y campos obligatorios incorrectos en la respuesta 400; y cuerpo declarado para una respuesta 204.

## Aserciones comunes

- El estado es uno de: `APROBADO`, `APROBADO CON OBSERVACIONES`, `REQUIERE AJUSTES` o `BLOQUEADO`.
- Separa errores de recomendaciones y respalda cada diferencia con una fuente y ubicacion comprobables.
- Revisa codigo de estado HTTP, encabezados, tipo de contenido y cuerpo para el alcance indicado.
- No inventa requisitos, fuentes consultadas, resultados de CI ni validaciones externas.
- No modifica contratos ni genera codigo.

## Criterio de aprobacion

El skill pasa cuando CT-01 a CT-07 y las aserciones comunes cumplen sus resultados esperados. Registrar por caso el prompt, el resultado obtenido y PASS/FAIL. No declarar el skill aprobado si falla la verificacion del nombre o cualquier caso obligatorio.