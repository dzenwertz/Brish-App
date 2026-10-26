# Comprobación de fechas de Brish sin Firebase

Este proyecto Kotlin/JVM compila directamente el archivo `Android/app/src/main/java/com/katchy/focuslive/ui/courses/CourseDates.kt`. No copia su lógica ni modifica la aplicación Android.

Sirve para revisar el manejo de fechas de cursos desde un clon público con JDK 17, sin Android SDK, cuenta, emulador, claves o configuración Firebase. La primera ejecución descarga Gradle y dependencias de Kotlin/JUnit.

Desde la raíz del repositorio, en Windows:

```powershell
.\Android\gradlew.bat -p verification test --console=plain
```

En Linux/macOS:

```bash
bash Android/gradlew -p verification test --console=plain
```

## Casos comprobados

- Las dos pruebas existentes de `CourseDatesTest`: conversión UTC/Lima y diferencia entre días locales.
- Cambio de año cerca de medianoche en Lima.
- Fecha seleccionada en un año bisiesto.
- Días consecutivos que duran 23 o 25 horas por horario de verano en Nueva York.

El reporte HTML queda en `verification/build/reports/tests/test/index.html`; los resultados XML, en `verification/build/test-results/test/`. GitHub Actions ejecuta la misma comprobación y guarda el reporte.

## Alcance

Solo verifica los helpers de fechas y los casos mencionados. No verifica Compose, pantallas, Firebase Auth, sincronización, notificaciones, pagos, firma ni un APK funcional.

Las pruebas Android completas mantienen sus requisitos de configuración propia. Omitir `processDebugGoogleServices` no es un reemplazo fiable: en la revisión local del 26/10/2026, el intento falló en `mapDebugSourceSetPaths` al faltar la salida generada por Google Services. No se añaden archivos de Firebase ficticios al repositorio para ocultar ese bloqueo.
