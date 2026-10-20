package com.katchy.focuslive.ui.theme

import androidx.compose.ui.graphics.Color

// Brish 2.0 — crema cálido, tinta y amarillo (ver design/visual-system.md)
val BrishCream = Color(0xFFF6F1E6)          // Fondo de pantalla
val BrishCard = Color(0xFFFFFDF9)           // Tarjetas y barras
val BrishInset = Color(0xFFEDE5D5)          // Superficies hundidas (selector Día/Semana)
val BrishInsetSoft = Color(0xFFEFE8DA)      // Vista previa, pistas vacías
val BrishHairline = Color(0xFFF0E9DB)       // Separadores dentro de tarjetas
val BrishInk = Color(0xFF1F2328)            // Texto principal y botón oscuro
val BrishTextSecondary = Color(0xFF5B5348)  // ≈7:1 sobre crema
val BrishTextTertiary = Color(0xFF6E655A)   // Solo texto pequeño sobre tarjeta
val BrishYellow = Color(0xFFF8D468)         // Acento: indicador, píldoras activas
val BrishYellowLight = Color(0xFFFCE7A2)    // Inicio del degradado destacado
val BrishYellowDeep = Color(0xFFF5C84A)     // Final del degradado de botones
val BrishYellowSoft = Color(0xFFFBEBB8)     // Fondos de iconos
val BrishOnYellow = Color(0xFF3A2F12)       // Texto secundario sobre amarillo
val BrishWarning = Color(0xFFB4530A)        // "vence vie"
val BrishNow = Color(0xFFE2412B)            // Línea de ahora

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
val BrishNight = Color(0xFF17140F)
val BrishNightCard = Color(0xFF221E18)
val BrishNightInset = Color(0xFF2C2720)
val BrishNightText = Color(0xFFF4EFE6)
val BrishNightTextSecondary = Color(0xFFBDB3A4)

// Nombres anteriores, conservados para las pantallas que aún los usan
val AntiFlashWhite = BrishCream
val AntiWhite = BrishCard
val AntiBlack = BrishInk
val AntiTextPrimary = BrishInk
val AntiTextSecondary = BrishTextSecondary
val AntiPrimary = BrishInk
val AntiAccent = BrishInset
val AntiSuccess = AreaHealth
val AntiError = Color(0xFFC2361F)
val GradientStart = BrishYellowLight
val GradientEnd = BrishYellow
