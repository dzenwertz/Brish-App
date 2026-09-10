package com.katchy.focuslive.ui.components.mascot

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.Easing

/**
 * Curvas de atenuación premium reutilizables para las micro-animaciones de las mascotas.
 *
 * Reglas de uso:
 *  - [EaseInOutSineSoft]  → respiración, balanceo, todo lo "orgánico" cíclico.
 *  - [EaseOutBackSoft]    → "settle" de un saludo o de un rebote (overshoot suave).
 *  - [EaseOutBackOvershoot]→ "land" del salto de entrada (overshoot marcado, Duolingo-like).
 *  - [EaseOutQuart]       → transiciones de props (cangrejo/bate/clipboard subiendo/bajando).
 *  - [EaseInOutCubicSoft] → transiciones largas (cambio de pose).
 *  - [SpringWave]         → spring suave para el "wave" final del saludo (oscila y asienta).
 *
 * Reservado `LinearEasing` solo para rotaciones continuas (propellers, stopwatch).
 */
object BrishEasings {

    /** Overshoot suave (1.4). Para "settle" de elementos que llegan a su posición. */
    val EaseOutBackSoft: Easing = CubicBezierEasing(0.34f, 1.42f, 0.64f, 1f)

    /** Overshoot marcado (1.8). Para el "land" elástico del salto de entrada. */
    val EaseOutBackOvershoot: Easing = CubicBezierEasing(0.2f, 1.8f, 0.4f, 1f)

    /** In-Out sinusoidal suave. Para todo orgánico cíclico (respirar, balance, orejas, cola). */
    val EaseInOutSineSoft: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

    /** In-Out cúbico. Para transiciones largas (cambio de pose, entrada de accesorios). */
    val EaseInOutCubicSoft: Easing = CubicBezierEasing(0.65f, 0.05f, 0.36f, 1f)

    /** Out quart. Para "subir" props (bate, clipboard) con有为 settle. */
    val EaseOutQuart: Easing = CubicBezierEasing(0.165f, 0.84f, 0.44f, 1f)

    /** Spring para el "wave" del saludo: baja stiffness + dampingRatio medio-bajo → oscila. */
    fun <T> springWave(): SpringSpec<T> = SpringSpec(
        dampingRatio = 0.45f,
        stiffness = Spring.StiffnessLow
    )

    /** Spring "snappy" para micro-ajustes (cabeza mira pointer, oreja twitch settle). */
    fun <T> springSnappy(): SpringSpec<T> = SpringSpec(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow
    )
}