package com.katchy.focuslive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Brish 2.4 — minimalista: grises de sistema, tinta y un único acento azul.
// Los nombres «Yellow» se conservan por compatibilidad; ahora son la selección azul suave.
val BrishAccent = Color(0xFF007AFF)         // Acento: botones principales, enlaces, foco
val BrishAccentDark = Color(0xFF0A84FF)     // Acento en modo oscuro
val BrishAccentSoft = Color(0xFFDCEAFF)     // Fondo de selección
val BrishCream = Color(0xFFF2F2F7)          // Fondo agrupado (como Ajustes de iOS)
val BrishCard = Color(0xFFFFFFFF)          // Tarjetas y barras
val BrishInset = Color(0xFFE9E9EF)          // Selector segmentado y superficies hundidas
val BrishInsetSoft = Color(0xFFF2F2F7)          // Vista previa, campos
val BrishHairline = Color(0xFFE5E5EA)          // Separadores
val BrishInkLight = Color(0xFF1C1C1E)
val BrishInkNight = Color(0xFFF2F2F7)
/** Texto principal (se adapta a claro/oscuro) */
val BrishInk: Color @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BrishInkNight else BrishInkLight
val BrishTextSecondary = Color(0xFF6C6C70)          // Texto secundario ≈5:1 sobre el fondo
val BrishTextTertiary = Color(0xFF76767B)          // Texto pequeño sobre tarjeta
val BrishSelectLight = Color(0xFFDCEAFF)
val BrishSelectNight = Color(0xFF1A3A66)
/** Selección suave */
val BrishYellow: Color @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BrishSelectNight else BrishSelectLight
val BrishYellowLight = Color(0xFFFFFFFF)          // Superficie destacada: ahora blanca y plana
val BrishYellowDeep = Color(0xFFDCEAFF)          // Botones suaves
val BrishYellowSoftLight = Color(0xFFEEF4FF)
val BrishYellowSoftNight = Color(0xFF1C2A40)
/** Fondos de iconos */
val BrishYellowSoft: Color @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BrishYellowSoftNight else BrishYellowSoftLight
val BrishOnYellowLight = Color(0xFF3C3C43)
val BrishOnYellowNight = Color(0xFFAEAEB2)
/** Texto secundario sobre superficies destacadas */
val BrishOnYellow: Color @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BrishOnYellowNight else BrishOnYellowLight
val BrishWarning = Color(0xFFC2410C)          // "vence vie"
val BrishNow = Color(0xFFFF3B30)          // Línea de ahora

// Áreas de vida: fondo suave + texto legible
val AreaWorkBg = Color(0xFFDCE7FB)
val AreaWork = Color(0xFF24408F)
val AreaHealthBg = Color(0xFFDDF1E5)
val AreaHealth = Color(0xFF1F6B45)
val AreaPersonalBg = Color(0xFFECE3FA)
val AreaPersonal = Color(0xFF5B3BA8)
val AreaHomeBg = Color(0xFFFCE6CC)
val AreaHome = Color(0xFF9A4A12)

// Modo oscuro
val BrishNight = Color(0xFF000000)          // Fondo oscuro
val BrishNightCard = Color(0xFF1C1C1E)          // Tarjetas en oscuro
val BrishNightInset = Color(0xFF2C2C2E)          // Hundidas en oscuro
val BrishNightText = Color(0xFFF2F2F7)          // Texto en oscuro
val BrishNightTextSecondary = Color(0xFFAEAEB2)          // Secundario en oscuro

// Superficies de acento, sombras, velos y partículas de celebración
val BrishWarmWhiteLight = Color(0xFFF2F2F7)
val BrishWarmWhiteNight = Color(0xFF2C2C2E)
/** Botón gris sobre tarjetas */
val BrishWarmWhite: Color @Composable @ReadOnlyComposable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BrishWarmWhiteNight else BrishWarmWhiteLight
val BrishGoldShadow = Color(0xFF000000)          // Sombra neutra
val BrishShadowTone = Color(0xFF000000)          // Sombra neutra
val BrishHeroGradientEnd = Color(0xFFFFFFFF)          // Superficie destacada plana
val BrishButtonGradientStart = Color(0xFFDCEAFF)          // Botón suave
val BrishScrim = Color(0x47000000)          // Velo suave de hojas y diálogos
val BrishScrimDeep = Color(0x73000000)      // Velo de guía interactiva
val BrishScrimDark = Color(0xD9000000)      // Velo oscuro de visor de fotos
val BrishOrbCoral = Color(0xFFE8F0FF)          // Luz ambiental en Enfoque (sutil)
val BrishOrbBlue = Color(0xFFD6E6FF)          // Luz ambiental en Enfoque (sutil)

// Tokens auxiliares de esquema claro/oscuro/AMOLED
val BrishOutlineLight = Color(0xFFC7C7CC)          // Contornos
val BrishSecondaryContainerDark = Color(0xFF1A3A66)          // Selección en oscuro
val BrishTertiaryDark = Color(0xFFFF9F0A)          // Terciario en oscuro
val BrishAmoledSurface = Color(0xFF0D0D0F)
val BrishAmoledSurfaceVariant = Color(0xFF1C1C1E)

// Partículas de celebración (BrishCelebrationBurst)
val BrishSparkMint = Color(0xFF4ADE80)
val BrishSparkAmber = Color(0xFFFBBF24)
val BrishSparkSky = Color(0xFF60A5FA)
val BrishSparkPink = Color(0xFFF472B6)

// Cajas Leitner para estudio de flashcards (armonizadas con áreas Brish)
val BrishLeitnerBox1 = BrishNow
val BrishLeitnerBox2 = BrishWarning
val BrishLeitnerBox3 = BrishYellowDeep
val BrishLeitnerBox4 = AreaHealth
val BrishLeitnerBox5 = AreaWork

// Paleta de cursos por defecto (enteros ARGB para persistencia en modelo/preferencias)
const val BrishDefaultCourseColorHex: Long = 0xFF24408FL
val BrishCoursePaletteHex: List<Long> = listOf(
    0xFF24408FL, // Azul Trabajo
    0xFF1F6B45L, // Verde Salud
    0xFF5B3BA8L, // Violeta Personal
    0xFF9A4A12L, // Terracota Hogar
    0xFFB4530AL, // Ámbar Cálido
    0xFF1F2328L  // Tinta Brish
)

// Nombres anteriores, conservados para las pantallas que aún los usan
val AntiFlashWhite = BrishCream
val AntiWhite = BrishCard
val AntiBlack = BrishInkLight
val AntiTextPrimary = BrishInkLight
val AntiTextSecondary = BrishTextSecondary
val AntiPrimary = BrishInkLight
val AntiAccent = BrishInset
val AntiSuccess = AreaHealth
val AntiError = Color(0xFFD70015)
val BrishDanger = AntiError
val BrishMuted = BrishTextSecondary
val GradientStart = BrishYellowLight
val GradientEnd = BrishSelectLight
