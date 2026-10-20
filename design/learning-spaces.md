# Brish: organización vigente

La navegación principal es Hoy, Agenda, Cursos y Notas. Ajustes contiene apariencia, Pomodoro y exportación de datos anteriores. Enfoque se abre desde Hoy o una actividad/entrega.

## Límites entre funciones

- Agenda es un horario de actividades creadas manualmente. Las entregas académicas no se insertan automáticamente.
- Cursos usa Espacio → Curso → Entregas y apuntes. Un espacio puede ser un ciclo universitario o una formación independiente.
- Notas es la biblioteca. Un apunte de curso mantiene el mismo identificador en ambas vistas; carpeta y curso son vínculos independientes.
- Hoy resume actividades y entregas en bloques separados. No convierte notas en actividades ni duplica entregas.

## Reglas del contenido

- Crear dentro de una carpeta o curso hereda esa ubicación. Los filtros Todas y Sin carpeta no son contenedores persistentes.
- Editor compartido de pantalla completa, autoguardado local tras 500 ms de pausa, borrador duradero y escritura pendiente finalizada antes de salir.
- El estado de guardado confirma la escritura local. La sincronización en segundo plano tiene otro estado y no condiciona escribir sin conexión.
- Las notas pueden tener contenido sin título. Abrir y cerrar un editor vacío no crea una nota.
- Papelera persistente; restaurar es posible desde Notas aunque el apunte se eliminó desde Cursos.
- Una entrega sin fecha no vence ni programa alarmas. Sigue disponible en Cursos y para enfoque.
- Archivar un espacio conserva relaciones y contenido. Eliminar un espacio o curso requiere que esté vacío.
- Cerrar sesión conserva datos locales por propietario, incluidos los pendientes y borradores.

## Funciones retiradas

Finanzas y Hábitos ya no tienen pantallas de trabajo; sus registros se conservan y pueden exportarse desde Ajustes. IA está pospuesta y no tiene entradas visibles. Mascotas, XP, monedas, insignias, rachas y recompensas se retiraron del código funcional.

## Verificación

Las pruebas cubren separación entre actividades y entregas, validación de horario, autoguardado, fallos de escritura, contexto de carpeta/curso, fechas opcionales y rechazo de eliminación no vacía. El pulido visual debe comprobarse en dispositivo: modo claro/oscuro, fuente ampliada, teclado, nombres largos, listas vacías y navegación atrás.
