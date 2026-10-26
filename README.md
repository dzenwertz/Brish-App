<div align="center">

# Brish

**Tu día, organizado en un solo lugar.**

Agenda, Pomodoro, hábitos, notas y finanzas personales en una aplicación Android nativa.

[RETH](https://rethagency.web.app/) · [Código fuente](https://github.com/dzenwertz/Brish-App)

</div>

## Descargar

[**Descargar Brish v1.2.22 para Android**](https://github.com/dzenwertz/Brish-App/releases/download/v1.2.22/Brish-v1.2.22.apk)

APK firmado para Android 7.0 o posterior. La configuración de clave personal de OpenCode está disponible en el código actual y llegará al próximo APK.

## Qué problema resuelve

Brish evita repartir la organización diaria entre varias aplicaciones. Une planificación, enfoque y seguimiento personal en una interfaz limpia y configurable.

## Funciones principales

- Agenda diaria con tareas, horarios y subtareas.
- Temporizador Pomodoro con sesiones enlazadas a tareas.
- Hábitos con rachas y progreso diario.
- Notas organizadas por carpetas.
- Registro sencillo de finanzas personales.
- Brish IA para ordenar ideas y proponer acciones revisables.

## Vistas

<p align="center">
  <img src="docs/screenshots/planner.jpeg" width="30%" alt="Agenda de Brish" />
  <img src="docs/screenshots/habits.jpeg" width="30%" alt="Hábitos en Brish" />
  <img src="docs/screenshots/notes.jpeg" width="30%" alt="Notas en Brish" />
</p>

## Brish IA y privacidad

Cada persona conecta su propia clave de [OpenCode Zen](https://opencode.ai/docs/zen/). La clave se cifra con Android Keystore y permanece en el dispositivo. Los modelos gratuitos dependen de la disponibilidad y las condiciones vigentes de OpenCode.

La IA propone cambios; el usuario los revisa y confirma antes de aplicarlos.

## Ejecutar el proyecto

1. Abre la carpeta `Android` en Android Studio.
2. Añade tu propio `Android/app/google-services.json` para las funciones de Firebase.
3. Define `sdk.dir` en `Android/local.properties` si Android Studio no lo hace automáticamente.
4. Sincroniza Gradle y ejecuta la variante `debug`.

Requisitos: Android Studio, JDK 17 y Android SDK 35.

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
