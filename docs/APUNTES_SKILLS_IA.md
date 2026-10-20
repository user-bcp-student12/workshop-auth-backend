# Ayuda memoria: skills para IA

## Que es un skill

Un skill es un conjunto reutilizable de instrucciones y conocimiento que orienta a un asistente de IA cuando trabaja en un tipo de tarea. Puede definir pasos, criterios, restricciones y ejemplos para que el trabajo sea consistente.

Un skill no es codigo que ejecute la aplicacion ni una funcion de negocio. Orienta al asistente; por si solo no ejecuta un flujo ni modifica el producto.

## Ubicacion y estructura

Para skills de GitHub Copilot en un repositorio, la ubicacion habitual es:

```text
.github/skills/<nombre-del-skill>/SKILL.md
```

`<nombre-del-skill>` es un marcador, no texto literal. En este proyecto, el nombre es `feature-intake-spec-checklist`. Esta ruta es una convencion reconocida por Copilot, no una estructura universal para todas las herramientas de IA.

Estructura relacionada de este proyecto:

```text
.github/skills/feature-intake-spec-checklist/
|-- SKILL.md
`-- examples/
    |-- input.sample.md
    `-- output.sample.md
docs/
|-- APUNTES_SKILLS_IA.md
`-- feature-intake-transfer-confirmation.md
```

`SKILL.md` contiene las instrucciones principales del skill. Los archivos de apoyo pueden organizarse de otra manera; lo importante es mantenerlos claros y referenciarlos desde las instrucciones cuando deban utilizarse. Esta ayuda memoria es documentación para el equipo y no se carga automáticamente como parte del skill.

## Para que sirven los ejemplos

- `examples/input.sample.md` muestra una solicitud de ejemplo que puede servir como entrada.
- `examples/output.sample.md` muestra el formato o nivel de detalle esperado en la respuesta.
- Los nombres `input.sample.md` y `output.sample.md`, y la carpeta `examples/`, son convenciones de este proyecto, no nombres obligatorios ni comandos especiales.
- Los ejemplos no forman automaticamente un proceso de entrada y salida. El `SKILL.md` debe explicar como usarlos.

## Que significa el nombre

`feature-intake-spec-checklist` se puede traducir como **lista de verificacion para relevar y especificar una funcionalidad**:

- `feature`: funcionalidad o mejora del producto.
- `intake`: recepcion y entendimiento inicial de la solicitud.
- `spec checklist`: lista para comprobar que la especificacion este completa.

## Proposito de este skill

Ayuda a convertir una solicitud inicial en una definicion revisable antes de implementar. En este caso, organiza alcance, supuestos, decisiones tecnicas, riesgos, criterios de aceptacion, validacion, tareas por rol y preguntas abiertas. Se usa antes de planificar una implementacion significativa; no esta pensado para incidentes P1 ni cambios triviales.