# Brish — ficha para la web de RETH

![Brish](logo/brish-logo-512.png)

## Descripción

Brish es una app Android para organizar el día sin ruido: reúne en un solo lugar la agenda, las tareas, las notas y los hábitos, con una captura rápida que entiende lo que escribes en lenguaje natural («pagar el internet el viernes 6pm #hogar») y lo convierte en un evento, una tarea, una nota o un hábito. Funciona sin conexión, sincroniza con la cuenta cuando hay internet y tiene un diseño minimalista y compacto, con modo claro y oscuro.

## Características clave

1. **Hoy, de un vistazo.** El bloque en curso con su progreso, lo que sigue, los pendientes y los hábitos del día.
2. **Captura en lenguaje natural.** Escribe o dicta como hablas; Brish detecta fecha, hora, área de vida y repetición.
3. **Agenda y notas en un mismo sitio.** Vista de día y semana con huecos libres, y notas con listas, carpetas, fotos y bloqueo con huella.
4. **Hábitos sin presión y modo Enfoque.** Metas semanales y contadores diarios, más un temporizador para concentrarse.

## Stack técnico

Kotlin 2.0 · Jetpack Compose · Hilt · Room · Firebase (Auth, Firestore, Crashlytics) · WorkManager · widgets Glance · Android 7.0+

## Datos de publicación

| Campo | Valor |
|---|---|
| Versión de esta entrega | 2.4.0 (versionCode 31) |
| Última versión publicada | 2.4.0 |
| Descarga | [Brish v2.4.0 (APK, GitHub)](https://github.com/dzenwertz/Brish-App/releases/download/v2.4.0/Brish-v2.4.0.apk) |
| Requisitos | Android 7.0 (API 24) o superior |
| Licencia | Propietaria. © 2026 Joseph Ivan Cachay Vargas / RETH. Todos los derechos reservados (ver `NOTICE.md`). |
| Demo | No hay demo web: es una app nativa de Android. |

## Capturas

Capturas reales del teléfono con datos de demostración; se recortó la barra de estado para no mostrar notificaciones ni datos personales.

| Archivo | Pantalla |
|---|---|
| `capturas/01-hoy.png` | Hoy: bloque en curso, lo que sigue y pendientes |
| `capturas/02-agenda.png` | Agenda del día con la hora actual y huecos libres |
| `capturas/03-notas.png` | Notas con búsqueda, filtros y carpetas |
| `capturas/04-habitos.png` | Hábitos con progreso semanal y contadores |
| `capturas/05-captura.png` | Captura interpretando «Pagar el internet el viernes 6pm #hogar» |

## Logo

`logo/brish-logo.svg` (vectorial) y `logo/brish-logo-512.png`. Se generan con `design/logo/generar_logo.py`.
