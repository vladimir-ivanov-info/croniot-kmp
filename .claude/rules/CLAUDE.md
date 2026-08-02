# Croniot KMP project

## Objetivo
Este proyecto prioriza mantenibilidad, claridad y arquitectura limpia sobre velocidad de entrega.

## Stack
- Kotlin
- Jetpack Compose
- Coroutines / Flow
- MVI
- Room
- Koin
- Gradle Kotlin DSL

## Reglas generales
- No introducir lógica en Composables si puede vivir en ViewModel / UseCase.
- Explicar siempre trade-offs antes de refactors grandes.
- Si propones una solución, compara al menos 2 alternativas cuando afecte arquitectura.
- No escribas código “mágico”: nombra claramente estados, eventos y efectos.
- Antes de tocar varios archivos, resume el plan en pasos pequeños.
- Cuando encuentres un bug, explica causa raíz, no solo el parche.

## Modo aprendizaje
- No hagas cambios grandes sin explicarme primero el razonamiento.
- Después de cada implementación relevante, dame:
  1. qué patrón usaste,
  2. por qué,
  3. qué alternativa descartaste,
  4. qué debo aprender de este cambio.
