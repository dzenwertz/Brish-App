<div align="center">

# Brish

**Tu día, tu semana y tus ideas. En un solo lugar.**

Aplicación Android nativa para organizar la vida diaria: Hoy, Agenda, Notas y Hábitos, con una captura rápida que entiende lo que escribes.

[RETH](https://rethagency.web.app/) · [Código fuente](https://github.com/dzenwertz/Brish-App)

</div>

## Descargar

La última versión publicada es la [**2.4.0**](https://github.com/dzenwertz/Brish-App/releases/download/v2.4.0/Brish-v2.4.0.apk) (Android 7.0 o posterior). Se instala encima de cualquier versión anterior sin perder datos porque usa la misma firma.

## Qué hace

- **Hoy:** el bloque de ahora con su progreso, lo que sigue, pendientes, hábitos del día y la revisión semanal los domingos y lunes.
- **Agenda:** vista Día con línea de tiempo (huecos libres, arrastrar para mover, notas enlazadas al día) y vista Semana en cuadrícula. Cada actividad tiene un área de vida: Trabajo, Salud, Personal u Hogar.
- **Captura (＋):** escribe como hablas («pagar la luz el viernes 6pm #hogar», «gym lun mie vie 5:30pm», «idea: …», «tomar agua 8 veces al día») y Brish crea la tarea, el evento, la nota o el hábito. También acepta dictado.
- **Notas:** búsqueda, fijadas, carpetas, listas con casillas que se reordenan arrastrando, fotos, enlace a un día y notas bloqueadas con huella o el bloqueo del teléfono.
- **Hábitos:** metas por semana y contadores diarios, sin rachas ni puntos, con aviso opcional a una hora.
- **Enfoque:** temporizador con sonidos, subtareas, +5 min y No molestar.
- **Revisión semanal:** resumen de la semana, pendientes para pasar a hoy o soltar, y carga de la semana siguiente.

Funciona sin conexión y sincroniza con la cuenta cuando hay internet. Las fotos de las notas se guardan solo en el teléfono.

## Diseño

Minimalista al estilo de iOS: fondo gris de sistema, tarjetas blancas planas, un único acento azul marino y modo oscuro coherente. Barra inferior fija, título grande que se pliega al hacer scroll, listas agrupadas, transiciones push/modal y respuesta táctil por atenuación. Iconos propios en línea fina (`design/icons`, SVG → VectorDrawable) y logo en `design/logo`. El tema vive en `Android/app/src/main/java/com/katchy/focuslive/ui/theme`.

## Ejecutar el proyecto

1. Abre la carpeta `Android` en Android Studio.
2. Añade tu propio `Android/app/google-services.json` para las funciones de Firebase.
3. Define `sdk.dir` en `Android/local.properties` si Android Studio no lo hace automáticamente.
4. Sincroniza Gradle y ejecuta la variante `debug`.

Requisitos: Android Studio, JDK 17 y Android SDK 35. Pruebas: `./gradlew :app:testDebugUnitTest`.

## Datos de versiones anteriores

Cursos, Finanzas, la IA y la mascota salieron del producto. Sus registros se conservan y Finanzas/Hábitos antiguos se pueden exportar a JSON desde Ajustes.

## Tecnologías y organización

- **Kotlin y Jetpack Compose:** interfaz Android nativa.
- **Room:** persistencia local.
- **Firebase Auth y Firestore:** servicios de cuenta y sincronización que requieren configuración propia.
- **Hilt, coroutines y WorkManager:** dependencias, tareas asíncronas y trabajo en segundo plano.
- **JUnit/MockK y pruebas de Compose:** herramientas de prueba presentes en el proyecto.

El código Android está en `Android/app/src/main/java/com/katchy/focuslive/`. Las pruebas JVM están en `Android/app/src/test/`.

## Comprobación y compilación

Para revisar la lógica de fechas desde un clon público sin Firebase ni Android SDK, basta JDK 17:

```powershell
.\Android\gradlew.bat -p verification test --console=plain
```

Esta comprobación compila el helper real de la aplicación y sus casos de fechas; [consulta su alcance y resultados](verification/README.md). No sustituye las pruebas de la aplicación completa.

Después de preparar Android Studio, JDK 17, SDK 35 y tu configuración de Firebase:

```powershell
cd Android
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

La compilación `release` necesita una clave de firma propia. No publiques `local.properties`, archivos de firma ni credenciales. Los comandos son una guía reproducible; no constituyen un resultado de pruebas de esta revisión documental.

## Qué demuestra este proyecto

Organización de una aplicación con varios módulos funcionales, persistencia, manejo de estados y configuración de servicios. Es un proyecto personal de desarrollo; no equivale a experiencia corporativa de soporte o administración cloud.

## Autoría

Diseñado y desarrollado por **Joseph Ivan Cachay Vargas**, fundador de **RETH**.

Copyright © 2026 Joseph Ivan Cachay Vargas / RETH. Todos los derechos reservados. Consulta [NOTICE](NOTICE.md).
