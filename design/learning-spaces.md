# Espacios de aprendizaje — propuesta pendiente de implementación

## Situación comprobada

- El semestre actual es una preferencia global, no un contenedor independiente.
- Renombrarlo actualiza el semestre de todos los cursos. No conserva ciclos separados.
- No existe un límite de seis cursos; el contador visual se corrigió.
- Las tareas requieren un vencimiento numérico. La opción sin fecha necesita contemplarse en Agenda, Inicio, IA y recordatorios, además del formulario.

## Organización propuesta

Espacio → cursos → entregas y apuntes. Ejemplos de espacios: Ciclo 2026-2, Ciberseguridad, Inglés.

Cada espacio necesita identidad estable, nombre, propietario y estado archivado. Un curso debe referenciar esa identidad, no depender del nombre del espacio. La selección del espacio no debe modificar los demás cursos.

La cabecera actual se convierte en un selector. Su menú reúne crear, renombrar y archivar. Crear un espacio empieza vacío. Archivar conserva cursos, apuntes e historial; no equivale a completar pendientes. Eliminar requiere explicar y confirmar qué pasa con su contenido; empezar permitiendo eliminar solo espacios vacíos reduce ambigüedad.

Los datos existentes se migran al espacio inicial sin cambiar identificadores de cursos o tareas. Los espacios deben sincronizarse y respetar el cambio de cuenta igual que los cursos; no basta con guardarlos en preferencias locales.

## Entregas sin fecha

Representar explícitamente la ausencia de fecha, sin usar cero ni una fecha ficticia. Mostrar «Sin fecha límite» en Cursos; mantener las entregas pendientes consultables por IA y seleccionables para enfoque. No mostrarlas como vencidas, no asignarlas a hoy y no programar alarmas hasta que el usuario defina fecha/hora.

## Hábitos

Representan constancia (practicar, leer, repasar), no sustituyen cursos ni proyectos. Mantener esta distinción visible y evitar duplicar entregas automáticamente en Hábitos.

## Criterios de aceptación

- Crear dos espacios, cambiar entre ellos y renombrar uno no modifica el otro.
- Archivar y recuperar un espacio conserva todas sus relaciones.
- Un segundo dispositivo y un cambio de cuenta respetan propietario y contenido.
- Una entrega sin fecha no vence, no activa recordatorios y puede recibir fecha después.
- La IA distingue espacio, curso, hábito y entrega; solo afirma cambios tras su ejecución confirmada.
- Los controles secundarios quedan en el selector/menú y los formularios, no en botones adicionales permanentes.
