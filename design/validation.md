# Validación de Brish 1.3.0

La estructura de esta versión es Hoy · Agenda · Cursos · Notas. El código conserva los registros anteriores de Finanzas y Hábitos para exportación; no incluye sus pantallas ni recordatorios. La IA queda sin acceso en la interfaz y la gamificación se retira.

## Comprobaciones automáticas

Resultado del 2 de octubre de 2026: APK debug 1.3.0 generado; **83 pruebas unitarias y 10 pruebas de SQLite aprobadas**; pruebas instrumentadas compiladas. Lint terminó sin errores y conserva 137 advertencias, principalmente recursos sin uso, recomendaciones de API y versiones de dependencias. No se ejecutaron pruebas en dispositivo.

Desde `Android`, con JDK 17 y SDK 35:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
```

Desde la raíz del repositorio:

```powershell
python -m unittest discover -s Android/tests -v
```

Las pruebas unitarias cubren el editor, sus borradores y errores, la separación de actividades y entregas, fechas opcionales, validación de horarios, formularios, callbacks de escritura, recordatorios y recuperación del temporizador. Las pruebas de SQLite ejecutan el SQL de migración y comprueban que el esquema histórico migrado coincide con el esquema v14 exportado por Room, preservando contenido e identificadores. Hay una prueba instrumentada adicional que abre la base migrada con Room.

La compilación de pruebas instrumentadas no significa que se hayan ejecutado. No hay un dispositivo conectado en el entorno de implementación.

## Validación pendiente en Android

- Crear notas en dos carpetas distintas, cerrar con Atrás, rotar y reiniciar la app. Revisar título vacío y contenido recuperado.
- Editar un apunte desde Cursos y abrir la misma nota desde Notas.
- Forzar un error de almacenamiento: conservar texto y permitir reintentar sin duplicados.
- Enviar a papelera, reiniciar, restaurar y eliminar definitivamente. Eliminar una carpeta y verificar que el vínculo con el curso permanece.
- Comprobar que una entrega nunca aparece en Agenda y que una entrega sin fecha no aparece vencida ni genera alarma.
- Archivar/restaurar un espacio con contenido. Rechazar la eliminación de cursos y espacios no vacíos.
- Cerrar sesión sin conexión, cambiar de cuenta y volver: conservar pendientes y ocultar los de la otra cuenta.
- Verificar recordatorios tras editar, completar, eliminar, reiniciar y cambiar fecha/zona horaria. Pulsar una notificación de entrega debe abrir Cursos.
- Iniciar enfoque desde Hoy, Agenda y Cursos. Reabrir el proceso conserva el temporizador; tras reiniciar el dispositivo se recupera pausado desde el último punto guardado.
- Revisar modo claro/oscuro, fuente ampliada, teclado, nombres largos, listas vacías y navegación Atrás.
- Exportar registros históricos con y sin conexión y comprobar contenido del JSON. Consultar/restaurar compras anteriores.

Esta revisión en dispositivo es necesaria antes de considerar la versión lista para publicación.
