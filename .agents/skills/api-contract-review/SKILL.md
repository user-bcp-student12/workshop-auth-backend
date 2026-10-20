---
name: api-contract-generation
description: "Usar para revisar respuestas HTTP de una API contra el contrato, las convenciones del equipo y la documentacion oficial."
---

# Skill de revision y validacion de contratos de API

## Proposito

Validar respuestas HTTP contra el contrato y las fuentes aprobadas. Generar recomendaciones, no implementacion.

## Entradas

- Contrato de API y alcance de la revision.
- Convenciones del equipo y practicas aprobadas.
- Documentacion oficial aplicable en Confluence.
- Toma como modelo de contrato con buenas practicas: contrato-buenas-practicas.yaml

## Salidas

Informe breve con estado, errores y recomendaciones; incluye evidencia y ejemplos ficticios cuando ayuden.

## Flujo

1. Confirma alcance, version y fuentes disponibles; si falta una fuente necesaria, indicalo.
2. Revisa codigo HTTP, encabezados, tipo de contenido y cuerpo de respuestas exitosas y de error.
3. Compara con los requisitos y convenciones aprobados; cita la fuente y ubicacion de cada diferencia.
4. Separa errores de recomendaciones y asigna un estado de salida.

## Reglas

- No inventar requisitos; distinguir errores de recomendaciones y citar evidencia.
- Si las fuentes se contradicen o falta una fuente necesaria, dejarlo como pendiente y pedir confirmacion.
- No incluir secretos, credenciales ni datos personales; usar ejemplos ficticios.
- Mantener las convenciones del equipo. No modificar contratos ni implementar cambios.

## Adaptacion

Usar las convenciones y la version de API del equipo. Si Confluence no esta disponible, marcar esa validacion como pendiente.

## Criterio de salida

- `APROBADO`: sin errores contra requisitos aprobados.
- `APROBADO CON OBSERVACIONES`: solo quedan recomendaciones.
- `REQUIERE AJUSTES`: hay diferencias verificables.
- `BLOQUEADO`: faltan fuentes o decisiones para evaluar.

Entregar evidencia y preguntas pendientes. Detenerse para revision humana; no implementar.