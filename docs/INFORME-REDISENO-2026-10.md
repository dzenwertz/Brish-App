# Informe de Rediseño y Autoguardado Universal — Brish 2.3.0 (Octubre 2026)

## 1. Resumen Ejecutivo

Se completó la segunda vuelta de rediseño visual, sistema unificado de movimiento (`MotionTokens` + `SharedTransitionLayout` + micro-interacciones) y autoguardado universal (`DraftStore` + `PersistentDraftText` + doble capa en `NoteEditorViewModel`) para **Brish (`com.katchy.focuslive`) v2.3.0 (`versionCode = 29`)**:

1. **Autoguardado Universal y Cero Pérdida de Texto**:
   - Se auditaron los **19 puntos de entrada de texto** (`TextField`, `OutlinedTextField`, `BasicTextField`) en toda la aplicación (documentados en [`docs/AUTOGUARDADO.md`](AUTOGUARDADO.md)).
   - Se corrigió un defecto crítico en `PersistentDraftText.kt` donde `onDispose` / `ON_PAUSE` / `ON_STOP` volcaban `draft.value` (`initialValue`) a `DraftStore` incluso cuando el formulario no había sido editado o inmediatamente después de llamar a `clearAfterSave()` / `discard()`, lo que provocaba que un borrador recién guardado o descartado resucitara al cerrarse el diálogo/hoja.
   - Se completó la persistencia en `CaptureViewModel` tanto para el texto (`capture:input`) como para el tipo seleccionado (`capture:type`), con volcado síncrono en ciclo de vida (`flushDraft()`), indicador animado `"Guardado ✓"` (`1.2 s`, fade + slide `8 dp`) y aviso `"Borrador recuperado"`.
   - Se añadieron borradores persistentes a todas las superficies faltantes: subtareas en curso en `PlannerScreen`, tarjetas de repaso (`FlashcardsEditorDialog`) en `NoteEditorScreen`, y tarjetas de reflexión semanal (`ReflectionCard`) en `WeeklyReviewScreen`.
   - En `LoginScreen`, solo se persiste el correo/nombre (`login:email`, `login:name`, `login:reset-email`), mientras que la contraseña jamás se almacena en `DraftStore`.

2. **Sistema de Movimiento, Transiciones Compartidas y Micro-interacciones**:
   - Se centralizaron curvas, resortes y duraciones en `MotionTokens` (`ui/theme/Motion.kt`), incluyendo `Modifier.brishCascade(index, key)` (cascada con stagger de `48 ms`, máx. 6 elementos, `20.dp` + fade, y degradación automática a fade de `150 ms` cuando `rememberReducedMotion()` está activo).
   - Se implementó `BrishCelebrationBurst` (`ui/components/BrishUi.kt`, `<= 700 ms`) con overshoot pop + 6 partículas radiales en cascada y vibración ligera al completar tareas (`HomeScreen`, `AgendaViews`), entregas (`CourseDetailScreen`), hábitos (`HabitsScreen`, `HomeScreen`), subtareas y sesiones de enfoque (`FocusSessionScreen`).
   - Se conectaron transiciones compartidas (`SharedTransitionLayout` + `brishSharedBounds` / `brishSharedElement`) en `Hoy <-> FocusSession`, `NotesScreen <-> NoteEditorScreen`, `CourseDetailScreen <-> NoteEditorScreen`, `CoursesScreen <-> CourseDetailScreen` y píldora activa de filtros en `NotesScreen`, además de fade-through (`90 ms` salida + `210 ms` entrada con escala `0.98f -> 1f`) entre pestañas de `MainScreen`.
   - Se rediseñaron `OnboardingScreen` (ilustración con entrada `0.6f -> 1f` + 3 satélites orbitales suaves en `BrishArt.kt`, título con slide horizontal `24.dp` + cross-fade, y CTA inferior fijo siempre visible con `"Siguiente"` / `"Entrar a mi cuenta"` + `"¿No tienes cuenta? Regístrate"`) y `LoginScreen` (ola superior con drop-in de `420 ms`, tarjeta blanca con subida de `24.dp`, borde amarillo animado al enfocar campos y circular reveal consciente de modo oscuro).

---

## 2. Distinción: Trabajo Heredado del Agente Previo vs. Implementado en Esta Sesión

### 2.1. Heredado del Agente Previo (conservado y verificado)
- Respaldo previo intacto en `E:\01_Mis_Proyectos_y_Codigo\Proyectos\Wlaz_Brish 2\Android\respaldo-2.2.2-pre-motion\src` (**119 archivos**, verificado sin sobrescribir).
- Estructura base de `DraftStore.kt` (DataStore `brish_drafts` con debounce de `300 ms`, `flush()` y `flushBlocking()`).
- Estructura inicial de `Motion.kt` (`MotionTokens` base y `rememberReducedMotion()`) y `SharedElements.kt` (`LocalSharedTransitionScope`, `LocalNavAnimatedScope`, `brishSharedBounds`, `brishSharedElement`).
- Integración inicial de `rememberPersistentDraftText` en campos principales de `PlannerScreen`, `HabitEditSheet`, `DeliverableDialog`, `SettingsScreen` y `OnboardingScreen`.
- Incremento de versión en `app/build.gradle.kts` a `versionName = "2.3.0"` y `versionCode = 29`.

### 2.2. Corregido e Implementado en Esta Sesión
- **Corrección crítica en `PersistentDraftText.kt`**:
  - Se reemplazó la escritura incondicional en `ON_PAUSE`/`ON_STOP`/`onDispose` por `flushIfDirty()`, que solo persiste si `userEdited == true` y `value != initialValue`.
  - Se añadieron `hydrate()`, `hydrateBlocking()` y `scheduleWrite` directo en el setter de `value` para garantizar funcionamiento idéntico en Compose y en tests unitarios JVM.
- **Autoguardado completo en `CaptureViewModel.kt` y `CaptureSheet.kt`**:
  - Persistencia de `capture:type` además de `capture:input`, `flushDraft()` al pausar/cerrar, indicador `"Guardado ✓"` (`1.2 s`), corrección del orden `reset()` antes de `onDone()` en `save()`, transición container-transform con resorte (`MotionTokens.sheetSpring()`) y píldora deslizante en `TypeSelector`.
- **Nuevas superficies con borrador persistente**:
  - `NoteEditorScreen.kt`: Diálogo completo `FlashcardsEditorDialog` con `flashcard:{noteId}:front` y `flashcard:{noteId}:back`, `DraftRecoveryNotice` y confirmación al descartar; animación `"Guardado ✓"` (`1.2 s`, fade + slide `8 dp`) en `SaveStatus`.
  - `NoteEditorViewModel.kt`: Cancelación explícita de `debounce` antes de ejecutar `flush()`.
  - `WeeklyReviewScreen.kt`: Nuevo componente `ReflectionCard` con `weekly-review:answer:week-{startMs}` y `weekly-review:answer:next-{startMs}` + `DraftRecoveryNotice`.
  - `PlannerScreen.kt`: `DraftRecoveryNotice` y `clearAfterSave()` al agregar subtareas en `AddTaskSheet` y `TaskDetailSheet`.
  - `CoursesScreen.kt`: `DraftRecoveryNotice` sin condiciones restrictivas y confirmación `"¿Descartar borrador?"` en `AddCourseDialog`, `CreateSpaceDialog` y `RenameSpaceDialog`.
- **Sistema de Movimiento, `brishCascade`, `BrishCelebrationBurst` y `SharedElements`**:
  - `Motion.kt`: `Modifier.brishCascade(index, key)`, constantes `celebrationMs = 620`, `tabExitMs = 90`, `tabEnterMs = 210`.
  - `BrishUi.kt`: `BrishCelebrationBurst` con soporte por `trigger: Int` y `active: Boolean`.
  - `BrishArt.kt` y `OnboardingScreen.kt`: 3 satélites flotantes (`±4.dp`, ciclo `3.6 s`), entrada `0.6f -> 1f` con `MotionTokens.overshoot`, slide horizontal `24.dp` en títulos y CTA fijo en las 3 páginas.
  - `LoginScreen.kt`: Animación de ola superior (`headerWave`), subida de tarjeta (`cardRise`), borde de foco animado (`Transparent -> BrishYellow`), reveal circular adaptado a modo oscuro (`BrishNight` / `BrishCream`) y limpieza de funciones muertas.
  - `BrishBottomBar.kt` y `MainScreen.kt`: Escala `0.92f -> 1f` con `MotionTokens.pressSpring()`, expansión de ancho con `MotionTokens.sheetSpring()`, fade-through `90 ms / 210 ms` entre pestañas y `LocalNavAnimatedScope` en rutas de Cursos.
  - `CoursesScreen.kt`, `CourseDetailScreen.kt`, `NotesScreen.kt`, `HabitsScreen.kt`, `HomeScreen.kt`, `AgendaViews.kt`, `FocusSessionScreen.kt`: Integración de `brishSharedBounds`, `brishSharedElement`, `brishCascade` y `BrishCelebrationBurst`.
- **Ampliación de Pruebas Unitarias (`DraftStoreRecoveryTest.kt`)**:
  - Cobertura de extremo a extremo sobre `FilePersistence` simulando cierre abrupto y recreación de proceso para `CaptureViewModel` y `PersistentDraftText` en todas las superficies requeridas.

---

## 3. Tabla de Pantallas y Componentes Tocados

| Archivo | Qué se rediseñó / corrigió | Animaciones y micro-interacciones nuevas |
| :--- | :--- | :--- |
| `ui/theme/Motion.kt` | Tokens centrales de tiempo y curvas (`MotionTokens`) y modificador reutilizable `Modifier.brishCascade`. | Cascada con stagger `48 ms` (máx. 6 ítems), traslación vertical `20.dp -> 0.dp` + fade, y fallback a fade `150 ms` con Reduce Motion. |
| `ui/components/SharedElements.kt` | Helpers `brishSharedBounds` y `brishSharedElement` conectados a `MotionTokens.cardSpring()`. | Morphing con resorte (`dampingRatio = 0.82f`, `stiffness = 380f`) entre tarjetas y pantallas de detalle. |
| `ui/components/BrishUi.kt` | Nuevo componente `BrishCelebrationBurst` para celebrar hitos en `<= 700 ms`. | Pop con `MotionTokens.overshoot` (`0.84f -> 1f`) + 6 partículas radiales en cascada + háptica ligera (`TextHandleMove`). |
| `ui/components/BrishArt.kt` | Ilustraciones vectoriales de Onboarding, estados vacíos y cabeceras. | 3 satélites flotantes alrededor de la ilustración con desfase (`±4.dp`, `3.6 s`), pausados automáticamente en Reduce Motion. |
| `ui/components/PersistentDraftText.kt` | Holder de autoguardado para formularios con `hydrate()`, `hydrateBlocking()` y `flushIfDirty()`. | Aviso animado `DraftRecoveryNotice` (`"Borrador recuperado"`) con `fadeIn` + `slideInVertically`. |
| `ui/onboarding/OnboardingScreen.kt` | Layout de 3 pasos con CTA inferior fijo en todo momento y campo de nombre persistente (`onboarding:name`). | Escala `0.6f -> 1f` con overshoot en ilustración, slide horizontal `24.dp` + cross-fade en título y barra de progreso animada con resorte. |
| `ui/auth/LoginScreen.kt` | Pantalla de acceso/registro con persistencia exclusiva de correo/nombre y limpieza de código muerto. | Drop-in de ola superior (`420 ms`), cascada en textos, subida de `24.dp` en tarjeta blanca, borde de foco `Transparent -> BrishYellow`, shake horizontal en error y circular reveal hacia `Home`. |
| `ui/main/BrishBottomBar.kt` & `MainScreen.kt` | Barra inferior flotante y `NavHost` principal con `SharedTransitionLayout`. | Pulso de icono `0.92f -> 1f` (`pressSpring()`), expansión de píldora activa (`sheetSpring()`) y transición fade-through (`90 ms` exit / `210 ms` enter con escala `0.98f -> 1f`). |
| `ui/capture/CaptureSheet.kt` & `CaptureViewModel.kt` | Hoja de captura rápida con persistencia de texto y tipo, indicador `"Guardado ✓"` y diálogo de descarte. | Entrada/salida con `MotionTokens.sheetSpring()` + `scaleIn` desde el botón `＋`, píldora deslizante en `TypeSelector` y cross-fade de chips detectados. |
| `ui/home/HomeScreen.kt` & `ui/focus/FocusSessionScreen.kt` | Tarjeta hero `Ahora`, filas de pendientes, chips de hábitos y pantalla de enfoque con anillo de progreso. | Transición compartida `focus-hero`, cascada `brishCascade`, odómetro vertical por dígito, orbes cálidos en movimiento y `BrishCelebrationBurst` al completar tareas/hábitos/subtareas/enfoque. |
| `ui/planner/PlannerScreen.kt` & `AgendaViews.kt` | Vistas Día/Semana/Lista y hojas de creación, edición y detalle de bloques. | `brishCascade` en filas de agenda, `BrishCelebrationBurst` al marcar bloques y autoguardado completo de tarea y subtareas. |
| `ui/notes/NotesScreen.kt` & `NoteEditorScreen.kt` | Lista de apuntes, filtros por carpeta, editor con checklist y nuevo `FlashcardsEditorDialog`. | `brishSharedBounds("note-{id}")` tarjeta-a-editor, `brishSharedElement("notes-filter-active-bg")` en píldora de filtro, `brishCascade` en tarjetas y animación `"Guardado ✓"` (`1.2 s`, fade + slide `8 dp`). |
| `ui/courses/CoursesScreen.kt` & `CourseDetailScreen.kt` | Espacios de aprendizaje, tarjetas de cursos, detalle de curso y entregas. | `brishSharedBounds("course-{id}")` entre tarjeta y detalle, `brishSharedBounds("note-{id}")` hacia editor, `brishCascade` y `BrishCelebrationBurst` al completar entregas. |
| `ui/habits/HabitsScreen.kt` & `HabitEditSheet.kt` | Resumen semanal, tarjetas de hábitos con grilla L–D y hoja de edición con `DraftStore`. | `brishCascade` en resumen y tarjetas, y `BrishCelebrationBurst` al alcanzar la meta diaria o marcar el día. |
| `ui/review/WeeklyReviewScreen.kt` | Revisión semanal de 3 pasos con pila interactiva de pendientes y nuevas tarjetas `ReflectionCard`. | Barras animadas por área/día, contador ascendente (`CountUp`), física de arrastre en `CardStack`, `brishCascade` y persistencia de reflexión semanal. |
| `ui/settings/SettingsScreen.kt` | Pantalla de ajustes y diálogo de edición de nombre con `DraftStore`. | `brishCascade` en secciones y `DraftRecoveryNotice` en el diálogo de perfil. |

---

## 4. Resumen del Sistema de Autoguardado

El inventario completo línea por línea (`archivo:línea`, clave de borrador, qué se perdía antes y comportamiento actual ante guardar/cancelar/cerrar) se encuentra en:
- [`docs/AUTOGUARDADO.md`](AUTOGUARDADO.md)

Puntos clave verificados:
- **Escritura automática**: Debounce de `300 ms` en `DraftStore` al escribir + volcado bloqueante (`flushIfDirty()` / `flushDraft()`) en `ON_PAUSE`, `ON_STOP` y `onDispose`.
- **Editor de apuntes (`NoteEditorViewModel`)**: Doble capa con `NoteDraft` inmediato en Room + `Note` con debounce de `500 ms` y `flush()` en `ON_PAUSE`/`ON_STOP`/Back.
- **Recuperación transparente**: Hidratación automática al reabrir la pantalla/hoja/diálogo con indicador `"Borrador recuperado"` y opción de descartar.
- **Privacidad**: En `LoginScreen`, la contraseña nunca se escribe en `DraftStore`.

---

## 5. Verificación Final, Pruebas y APK Generado

### 5.1. Respaldo Previo Verificado
- Ruta: `E:\01_Mis_Proyectos_y_Codigo\Proyectos\Wlaz_Brish 2\Android\respaldo-2.2.2-pre-motion\src`
- Estado: **Intacto y completo** (`119` archivos originales de `app/src/main` y `app/src/test` preservados sin modificaciones).

### 5.2. Resultados de Compilación y Pruebas Unitarias
Comando ejecutado:
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```
- **Resultado de compilación (`:app:compileDebugKotlin`, `:app:assembleDebug`)**: `BUILD SUCCESSFUL` (0 errores).
- **Resultado de pruebas unitarias (`:app:testDebugUnitTest`)**:
  - **Suites ejecutadas**: `21`
  - **Tests totales**: `128`
  - **Fallos / Errores**: `0` (`100%` aprobados, incluyendo los 10 tests de recuperación ante muerte de proceso en `DraftStoreRecoveryTest` y los 9 tests de autoguardado con revisiones en `NoteEditorViewModelTest`).

### 5.3. APK Debug Generado
- **Ruta en `apks/`**: `E:\01_Mis_Proyectos_y_Codigo\Proyectos\Wlaz_Brish 2\apks\Brish-2.3.0-debug.apk` (también disponible en `Android\apks\Brish-2.3.0-debug.apk` y `Android\app\build\outputs\apk\debug\Brish_v2.3.0-debug.apk`).
- **Tamaño**: `72,167,181 bytes` (`68.82 MB`).
- **Versión**: `versionName = "2.3.0"`, `versionCode = 29`.

### 5.4. Verificación en Dispositivo / Emulador
- Se comprobó la disponibilidad de dispositivos mediante `adb devices`; `adb` no se encuentra en el `PATH` del entorno actual ni hay un emulador activo conectado a la sesión, por lo que la verificación de persistencia y ciclo de vida se validó exhaustivamente mediante la suite de pruebas unitarias sobre disco real (`FilePersistence` en `DraftStoreRecoveryTest` y `NoteEditorViewModelTest`) y el ensamblado completo del APK debug.

---

## 6. Actualización 2.3.1 (`versionCode = 30`) — Coherencia Visual Total y Micro-interacciones Vivas

### 6.1. Respaldo Previo `2.3.0`
- Ruta: `E:\01_Mis_Proyectos_y_Codigo\Proyectos\Wlaz_Brish 2\Android\respaldo-2.3.0-pre-motion\src` (**280 archivos** respaldados antes de iniciar `2.3.1`).

### 6.2. Unificación Visual (`Brish 2.0`) y Cero Colores Sueltos
- **Auditoría completa** documentada en [`docs/COHERENCIA-2.3.1.md`](COHERENCIA-2.3.1.md) con tabla detallada (`Pantalla · Qué estaba disparejo · Cómo quedó`).
- **Componentes compartidos en `ui/components/BrishUi.kt`**:
  - `BrishPrimaryButton`, `BrishSecondaryButton`, `BrishChoiceChip`, `BrishCardSurface`, `BrishTextField` (píldora con borde amarillo animado al enfocar como `LoginScreen`), `BrishScreenHeader`, `BrishEmptyState` (con ilustración `BrishArt`), `BrishDigitRoll` (odómetro vertical), `BrishPullToRefreshBox` (indicador amarillo `BrishYellow` sobre `BrishInk`) y `LocalTabReselectSignal`.
- **Eliminación del 100% de colores hexadecimales fuera de `ui/theme/`**:
  - Verificación automática con PowerShell (`Select-String -Pattern "Color\(0x|0xFF"` fuera de `ui/theme/`) = **0 coincidencias**.
  - Todos los tokens semánticos viven en `ui/theme/Color.kt` (`BrishGoldShadow`, `BrishScrimDeep`, `BrishLeitnerBox1..5`, `BrishSpark*`, `BrishWidget*`, `BrishCoursePaletteHex`, etc.).
- **Preservación de `LoginScreen.kt`**:
  - Se conservaron intactos los ajustes de Claude en `LoginScreen.kt` (encabezado amarillo con `statusBarsPadding()`, tarjeta blanca con `heightIn(min = maxHeight - 200.dp)`, enlace superior `"Registrarme"`/`"Iniciar sesión"` y círculo decorativo detrás del texto) y se eliminó el código muerto legado.

### 6.3. Nuevas Micro-interacciones y Física (`2.3.1`)
1. **Hoy (`HomeScreen.kt`, `HomeAgenda.kt`, `HomeViewModel.kt`)**:
   - `NowCard` con progreso en vivo actualizado segundo a segundo (`nowSecondOfMinute`) y barra de progreso en tiempo real cuando el temporizador de enfoque está corriendo.
   - Al completar una tarea: pop elástico del check + barrido animado del tachado (`strikeProgress`) + colapso de la fila con resorte (`MotionTokens.sheetSpring()`).
   - Swipe horizontal: deslizar a la derecha completa la tarea; deslizar a la izquierda la pospone para mañana (`postponeTaskToTomorrow`) mostrando snackbar con acción `"Deshacer"` (`undoPostponeTask`).
2. **Agenda (`AgendaViews.kt`, `PlannerScreen.kt`, `PlannerViewModel.kt`)**:
   - Drag-to-move de bloques con escala (`1.045f`) y sombra (`26.dp`) al levantar, háptica al cruzar cada cuarto de hora (`TextHandleMove`) y al soltar (`LongPress`), y resorte de "snap" (`Animatable` + `MotionTokens.sheetSpring()`) al encajar en la ranura de 15 minutos.
   - Transición Día↔Semana con `brishSharedBounds("agenda-block-{id}")`.
3. **Captura `＋` (`CaptureSheet.kt`, `CaptureParser.kt`, `MainScreen.kt`)**:
   - La hoja nace desde el botón `＋` (`TransformOrigin(0.5f, 0.94f)`).
   - `CaptureParser` detecta rangos (`CaptureTokenSpan`) de tipo/prefijo, fecha, hora, `#área` y frecuencia, y `CaptureField` los resalta en vivo con fade suave (`VisualTransformation`).
   - Al guardar, un chip (`FlyingCaptureChip`) vuela en arco desde la hoja hacia el tab correspondiente (`Hoy`, `Agenda`, `Notas` o `Hábitos`).
4. **Notas (`NotesScreen.kt`, `NoteEditorScreen.kt`, `FlashcardsStudyScreen.kt`)**:
   - Transición compartida `brishSharedBounds("note-{id}")` de ida y vuelta entre tarjeta y editor (sin colisión de claves entre fijadas y lista).
   - Icono de pin con bounce (`MotionTokens.overshoot()`) al fijar/desfijar en tarjeta y editor.
   - Checklists en `NoteEditorScreen`: check con pop elástico + `BrishCelebrationBurst`.
5. **Hábitos (`HabitsScreen.kt`, `HabitEditSheet.kt`)**:
   - Contador con `BrishDigitRoll` (estilo odómetro) y anillo que se llena (`HabitCounterRingButton` y `LiquidCheck` con `drawArc` animado con `MotionTokens.sheetSpring()`).
   - Al completar todos los hábitos programados del día, `WeekSummary` dispara `BrishCelebrationBurst` y llena su anillo diario.
6. **Enfoque (`FocusSessionScreen.kt`, `HomeViewModel.kt`)**:
   - `TimerRing` respira suavemente en escala y grosor mientras el temporizador está corriendo (`rememberInfiniteTransition`, pausado con `rememberReducedMotion()`).
   - Al terminar el tiempo automáticamente (`sessionCompletedEvent`), dispara `BrishCelebrationBurst` + vibración/háptica.
7. **Barra inferior (`BrishBottomBar.kt`, `MainScreen.kt`) y Pull-to-Refresh**:
   - El icono activo hace un bounce elástico al seleccionarse o re-tocarse; tocar el tab ya activo emite `LocalTabReselectSignal` y desplaza la lista al tope (`HomeScreen`, `AgendaViews`, `NotesScreen`, `CoursesScreen`, `HabitsScreen`).
   - Pull-to-refresh con indicador amarillo propio (`BrishPullToRefreshBox`) en `HomeScreen`, `PlannerScreen`, `NotesScreen`, `CoursesScreen` y `HabitsScreen`.

### 6.4. Verificación `2.3.1` (`:app:testDebugUnitTest :app:assembleDebug`)
- **Suites ejecutadas**: `21`
- **Tests totales**: `132` (`0` fallos, `0` errores; incluye nuevos tests para `detectCaptureSpans`, `postponeTaskToTomorrow`, progreso sub-minuto en `planToday` y `postponeTask` + `undoPostponeTask`).
- **APK Debug generado**: `E:\01_Mis_Proyectos_y_Codigo\Proyectos\Wlaz_Brish 2\apks\Brish-2.3.1-debug.apk` (`72,025,338 bytes` / `68.69 MB`, `versionName = "2.3.1"`, `versionCode = 30`).
