package com.katchy.focuslive.data.model

import androidx.compose.ui.graphics.Color

enum class MascotType(
    val id: String,
    val displayName: String,
    val role: String,
    val quote: String,
    val description: String,
    val accentColor: Color,
    val coinCost: Int,
    val isHidden: Boolean = false
) {
    POPPIN(
        id = "poppin",
        displayName = "Poppin",
        role = "EL COACH TÓXICO",
        quote = "\"¡Deja de ser gil y chapa tu ritmo!\"",
        description = "Te grita (con cariño) para que dejes de procastinar. Full pilas y directo a la yugular.",
        accentColor = Color(0xFFEF4444), // Red
        coinCost = 0,
        isHidden = false
    ),
    KITTY(
        id = "kitty",
        displayName = "Kitty",
        role = "LA PATRONA",
        quote = "\"Resultados quiero, no floro.\"",
        description = "Estricta, directa y sin pelos en la lengua. Si te relajas, te cae.",
        accentColor = Color(0xFF9CA3AF), // Gray
        coinCost = 50,
        isHidden = true
    ),
    PACO(
        id = "paco",
        displayName = "Paco",
        role = "EL PATA LEAL",
        quote = "\"Vamos con fe, causa. Tú eres.\"",
        description = "Tu brother incondicional. Siempre te apoya y te sube la moral cuando estás bajoneado.",
        accentColor = Color(0xFFF97316), // Orange
        coinCost = 100,
        isHidden = false
    ),
    PANDA(
        id = "panda",
        displayName = "Panda",
        role = "EL MAESTRO RELAX",
        quote = "\"Llévala suave, sin palta.\"",
        description = "Te enseña a chambear sin estresarte. Full vibras positivas y cero dramas.",
        accentColor = Color(0xFF1F2937), // Dark/Black
        coinCost = 150,
        isHidden = false
    ),
    CEBRIC(
        id = "cebric",
        displayName = "Cebric",
        role = "EL VISIONARIO",
        quote = "\"¡Alucina esa idea, bravazo!\"",
        description = "El creativo del grupo. Siempre pensando fuera de la caja y soñando en grande.",
        accentColor = Color(0xFF4ADE80), // Green
        coinCost = 150,
        isHidden = true
    ),
    TURTLE(
        id = "turtle",
        displayName = "Torty",
        role = "EL TÍO SABIO",
        quote = "\"Despacio se llega lejos, sobrino.\"",
        description = "Lento pero seguro. Te da consejos de viejo zorro para que no metas la pata.",
        accentColor = Color(0xFF0D9488), // Teal
        coinCost = 200,
        isHidden = false
    ),
    LLAMA(
        id = "llama",
        displayName = "Kuzco",
        role = "EL CAUSA",
        quote = "\"¡Habla barrio! ¡Al toque roque!\"",
        description = "El alma de la fiesta. Full jerga, full pilas y 100% peruano. Te pone las pilas en una.",
        accentColor = Color(0xFFD97706), // Amber/Brown
        coinCost = 0,
        isHidden = false
    ),
    TIGRICH(
        id = "tigrich",
        displayName = "Tigri'ch",
        role = "LA CAPA CREATIVA",
        quote = "\"¡Saca las garras por tus metas!\"",
        description = "Es una tigresa hermosa y la hermana de Cebric. Combina la garra y elegancia para guiar tu organización.",
        accentColor = Color(0xFFFBBF24), // Gold
        coinCost = 300,
        isHidden = true
    ),
    PINGUCH(
        id = "pinguch",
        displayName = "Pingu'ch",
        role = "EL DETECTIVE",
        quote = "\"Elemental, mi querido socio. Foco total.\"",
        description = "Un pingüino detective con abrigo y lupa. Analiza tus hábitos con frialdad y genialidad.",
        accentColor = Color(0xFF3B82F6), // Blue
        coinCost = 180,
        isHidden = true
    ),
    LUPO(
        id = "lupo",
        displayName = "Lupo",
        role = "EL TECH-WOLF",
        quote = "\"Optimiza tu día, el código está listo.\"",
        description = "Un lobo cyberpunk con visor de neón. El maestro de la tecnología y el enfoque de alto rendimiento.",
        accentColor = Color(0xFFA855F7), // Purple
        coinCost = 220,
        isHidden = true
    ),
    FOXY(
        id = "foxy",
        displayName = "Foxy",
        role = "LA ESTRATEGA",
        quote = "\"Cada paso es parte de la estrategia, confía.\"",
        description = "Una zorrita astuta y veloz. Diseña tácticas inteligentes para que logres tus objetivos sin perder tiempo.",
        accentColor = Color(0xFFF97316), // Fox Red
        coinCost = 160,
        isHidden = true
    ),
    KOKO(
        id = "koko",
        displayName = "Koko",
        role = "EL ZEN SENSEI",
        quote = "\"Desacelera para ver con más claridad.\"",
        description = "Un koala súper calmado y consciente. Te guía hacia la productividad consciente y la atención plena.",
        accentColor = Color(0xFF94A3B8), // Slate Gray
        coinCost = 210,
        isHidden = true
    ),
    DRACO(
        id = "draco",
        displayName = "Draco",
        role = "EL IMPULSOR CYBER",
        quote = "\"¡Enciende tus motores y conquista el día!\"",
        description = "Un dragón cibernético lleno de energía y fuego. Te impulsa con motivación de alto octanaje y tecnología.",
        accentColor = Color(0xFF0D9488), // Cyber Teal
        coinCost = 280,
        isHidden = true
    )
}
