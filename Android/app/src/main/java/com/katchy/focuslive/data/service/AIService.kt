package com.katchy.focuslive.data.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.katchy.focuslive.data.api.OpenCodeMessage
import com.katchy.focuslive.data.repository.OpenCodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class BrainDumpTask(
    val title: String,
    val description: String = "",
    val dueDateTimestamp: Long? = null,
    val time: String? = null, // Format HH:mm
    val estimatedMinutes: Int = 15,
    val priority: String = "MEDIUM"
)

@Singleton
class AIService @Inject constructor(
    private val openCodeRepository: OpenCodeRepository
) {
    private val gson = Gson()

    /**
     * Convierte texto libre en tareas estructuradas.
     * @param input Texto del usuario (ej: "Comprar leche y llamar a Juan mañana")
     * @return Lista de tareas estructuradas
     */
    suspend fun generateBrainDumpTasks(input: String): List<BrainDumpTask> = withContext(Dispatchers.IO) {
        val now = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val fullDateFormat = java.text.SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
        
        val todayDate = dateFormat.format(now.time)
        val currentTime = timeFormat.format(now.time)
        val readableDate = fullDateFormat.format(now.time)
        val currentTimeZone = java.util.TimeZone.getDefault().id

        val systemPrompt = """
            ERES UN ASISTENTE DE PRODUCTIVIDAD.
            Tu misión es extraer tareas concretas del siguiente texto.
            
            CONTEXTO TEMPORAL (CRÍTICO):
            - Hoy es: $readableDate ($todayDate).
            - Hora actual: $currentTime.
            - Zona horaria del dispositivo: $currentTimeZone.
            - Si el usuario dice "mañana", es el día siguiente a $todayDate.
            - Si dice "el viernes", es el próximo viernes.
            
            INSTRUCCIONES:
            1. Analiza el texto e infiere tareas accionables.
            2. Extrae fechas y horas explícitas o implícitas.
            3. Responde SOLAMENTE con un JSON Array válido.
            
            MODELO JSON:
            [
              {
                "title": "Titulo de la tarea",
                "date": "YYYY-MM-DD", 
                "time": "HH:mm", 
                "description": "Detalles extra",
                "priority": "HIGH" // "HIGH", "MEDIUM" o "LOW" segun urgencia/importancia
              }
            ]
            
            NOTA:
            - Si no menciona fecha, usa "$todayDate".
            - Si no menciona hora, usa null.
            - Si es urgente o prioritario, asigna "HIGH".
        """.trimIndent()

        try {
            
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", input)
            )

            val result = openCodeRepository.getChatCompletion(messages)
            val rawText = result.getOrNull() ?: "[]"
            val jsonText = rawText.extractJsonArray()
            
            // Parse the structured task response before exposing it to the UI.
            data class TempTaskDto(
                val title: String,
                val description: String = "",
                val date: String? = null,
                val time: String? = null,
                val estimatedMinutes: Int = 15,
                val priority: String? = null
            )

            val type = object : TypeToken<List<TempTaskDto>>() {}.type
            val dtos: List<TempTaskDto> = gson.fromJson(jsonText, type) ?: emptyList()
            
            // Map DTO to Domain Model with Timestamp calculation
            val tasks = dtos.map { dto ->
                var timestamp: Long? = null
                if (!dto.date.isNullOrBlank()) {
                    try {
                        val parsedDate = dateFormat.parse(dto.date)
                        val cal = java.util.Calendar.getInstance()
                        parsedDate?.let { cal.time = it }
                        
                        // Set specific time if available, otherwise default to "start of day" logic or just date
                         if (!dto.time.isNullOrBlank()) {
                            val parts = dto.time.split(":")
                            if (parts.size == 2) {
                                cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                                cal.set(java.util.Calendar.MINUTE, parts[1].toInt())
                            }
                        }
                        timestamp = cal.timeInMillis
                    } catch (e: Exception) {
                        android.util.Log.e("AIService", "Exception caught", e)
                    }
                }
                
                val cleanPriority = when (dto.priority?.uppercase()) {
                    "HIGH", "ALTA" -> "HIGH"
                    "LOW", "BAJA" -> "LOW"
                    else -> "MEDIUM"
                }

                BrainDumpTask(
                    title = dto.title,
                    description = dto.description,
                    dueDateTimestamp = timestamp,
                    time = dto.time,
                    estimatedMinutes = dto.estimatedMinutes,
                    priority = cleanPriority
                )
            }
            
            return@withContext tasks
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Error generating tasks with OpenCode", e)
            android.util.Log.e("AIService", "Exception caught", e)
            return@withContext emptyList()
        }
    }
    
    // Improved JSON Extractor - Aggressively looks for [ ... ] after cleaning thinking tags and markdown
    private fun String.extractJsonArray(): String {
        val cleaned = this.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("```(?:json)?"), "")
            .replace("```", "")
            .trim()
        val firstBracket = cleaned.indexOf('[')
        val lastBracket = cleaned.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return cleaned.substring(firstBracket, lastBracket + 1)
        }
        return "[]" // Fallback to empty list if no brackets found
    }

    /**
     * Reordena y prioriza tareas usando la Matriz de Eisenhower.
     */
    suspend fun prioritizeTasks(tasks: List<com.katchy.focuslive.data.model.Task>): List<PrioritizedTask> = withContext(Dispatchers.IO) {
        if (tasks.isEmpty()) return@withContext emptyList()

        val tasksJson = tasks.joinToString(separator = ", ") { 
             "{ \"id\": \"${it.id}\", \"title\": \"${it.title}\", \"category\": \"${it.category}\" }" 
        }

        val systemPrompt = """
            ERES UN EXPERTO EN GESTIÓN DE TIEMPO (Matriz de Eisenhower).
            Tu misión es asignar una prioridad (HIGH, MEDIUM, LOW) a cada tarea basándote en su URGENCIA e IMPORTANCIA.
            
            CRITERIOS:
            - HIGH (Urgente e Importante): Crisis, deadlines hoy, salud, pagos.
            - MEDIUM (Importante no Urgente): Planificación, ejercicio, mejoras.
            - LOW (Ni Urgente ni Importante): Entretenimiento, trivialidades.

            INPUT:
            [$tasksJson]

            INSTRUCCIONES:
            1. Analiza cada tarea.
            2. Devuelve un JSON Array con el ID y la nueva prioridad.
            3. Sé estricto. No todo puede ser HIGH.
            
            MODELO DE RESPUESTA JSON:
            [
              { "taskId": "ID_DE_LA_TAREA", "newPriority": "HIGH", "reason": "Es un pago con deadline." }
            ]
        """.trimIndent()

        try {
            android.util.Log.d("AIService", "Prioritizing ${tasks.size} tasks with OpenCode...")
            
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", "Prioriza estas tareas por favor.")
            )

            val result = openCodeRepository.getChatCompletion(messages)
            val rawText = result.getOrNull() ?: "[]"
            val jsonText = rawText.extractJsonArray()
            
            val type = object : TypeToken<List<PrioritizedTask>>() {}.type
            val prioritizedTasks: List<PrioritizedTask> = gson.fromJson(jsonText, type) ?: emptyList()
            
            return@withContext prioritizedTasks
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Exception caught", e)
            return@withContext emptyList()
        }
    }

    /**
     * Generates a breakdown of subtasks for a given task title.
     */
    suspend fun generateSubtasks(taskTitle: String): List<String> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            ERES UN ASISTENTE DE PRODUCTIVIDAD EXPERTO EN "GTD" (Getting Things Done).
            Tu misión es dividir una tarea grande en 3 a 5 pasos pequeños, concretos y accionables.
            
            INPUT: "$taskTitle"
            
            OUTPUT ESPERADO (JSON Array de Strings):
            ["Paso 1", "Paso 2", "Paso 3"]
            
            REGLAS:
            1. Solo JSON Array. Sin markdown.
            2. Pasos cortos (max 5 palabras).
            3. Si la tarea ya es muy simple, devuelve una lista vacía [].
        """.trimIndent()

        try {
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", "Divide esta tarea: $taskTitle")
            )

            val result = openCodeRepository.getChatCompletion(messages)
            val rawText = result.getOrNull() ?: "[]"
            val jsonText = rawText.extractJsonArray()
            
            val type = object : TypeToken<List<String>>() {}.type
            val subtasks: List<String> = gson.fromJson(jsonText, type) ?: emptyList()
            
            return@withContext subtasks
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Exception caught", e)
            return@withContext emptyList()
        }
    }

    /**
     * Resume un texto a un párrafo conciso en español.
     */
    suspend fun summarizeText(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        val systemPrompt = """
            ERES UN ASISTENTE DE PRODUCTIVIDAD EXPERTO EN SÍNTESIS.
            Tu misión es resumir el texto proporcionado por el usuario en un único párrafo conciso y directo en español.
            Mantén los puntos clave y elimina la redundancia.
            No agregues introducciones, explicaciones ni saludos. Solo responde con el texto resumido.
        """.trimIndent()
        try {
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", text)
            )
            val result = openCodeRepository.getChatCompletion(messages)
            return@withContext result.getOrNull()?.trim() ?: text
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Exception caught", e)
            return@withContext text
        }
    }

    /**
     * Corrige ortografía y gramática y mejora el estilo del texto en español sin cambiar su sentido original.
     */
    suspend fun polishText(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        val systemPrompt = """
            ERES UN EDITOR DE TEXTO EXPERTO EN ESPAÑOL.
            Tu misión es pulir el texto provisto por el usuario:
            - Corrige errores de ortografía, puntuación y gramática.
            - Mejora la claridad y fluidez del redactado sin cambiar el significado original ni la intención del autor.
            - Conserva el formato markdown (listas, negritas, etc.) si está presente en el original.
            - No agregues introducciones, comentarios ni explicaciones sobre lo que corregiste. Solo responde con el texto pulido resultante.
        """.trimIndent()
        try {
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", text)
            )
            val result = openCodeRepository.getChatCompletion(messages)
            return@withContext result.getOrNull()?.trim() ?: text
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Exception caught", e)
            return@withContext text
        }
    }

    /**
     * Estructura y mejora una nota completa de forma minimalista, limpia, sin asteriscos ni markdown sucio.
     */
    suspend fun enhanceNoteContent(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        val systemPrompt = """
            ERES UN ASISTENTE DE REDACCIÓN Y ESTRUCTURACIÓN DE NOTAS MINIMALISTA.
            Tu misión es estructurar, organizar y embellecer la nota del usuario:
            - Corrige ortografía, gramática y mejora la claridad y elegancia de las ideas.
            - Organiza el contenido en párrafos limpios y ordenados, o listas de guiones (-) si el contenido son puntos o pasos.
            - Conserva el 100% de la información original y datos del usuario sin inventar ni omitir nada.
            
            REGLAS ESTRICTAS DE FORMATO:
            1. CERO ASTERISCOS: NO uses asteriscos (**), no uses negritas en markdown que ensucien el texto.
            2. CERO EMOJIS INNECESARIOS: No agregues emojis a menos que el original los tenga.
            3. CERO INTRODUCCIONES NI SALUDOS: Responde estrictamente con el texto de la nota mejorado.
            4. IDIOMA: 100% español natural y pulcro.
        """.trimIndent()
        try {
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", text)
            )
            val result = openCodeRepository.getChatCompletion(messages)
            val raw = result.getOrNull()?.trim() ?: text
            val clean = raw.replace("*", "").replace(Regex("(?s)<think>.*?</think>"), "").trim()
            return@withContext if (clean.isNotBlank()) clean else text
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Exception in enhanceNoteContent", e)
            return@withContext text
        }
    }

    /**
     * Agrupa y organiza notas en secciones/categorías lógicas sin alterar su contenido.
     */
    suspend fun organizeNotes(
        notes: List<com.katchy.focuslive.data.model.Note>,
        categories: List<com.katchy.focuslive.data.model.NoteCategory>
    ): List<NoteOrganization> = withContext(Dispatchers.IO) {
        if (notes.isEmpty()) return@withContext emptyList()

        val notesJson = notes.joinToString(separator = ", ") { 
             "{ \"id\": \"${it.id}\", \"title\": \"${it.title}\", \"content\": \"${it.content.take(100)}\" }" 
        }
        val categoriesJson = categories.joinToString(separator = ", ") { "\"${it.name}\"" }

        val systemPrompt = """
            ERES UN ORGANIZADOR DE INFORMACIÓN EXPERTO.
            Tu misión es agrupar las notas del usuario en secciones o categorías lógicas y descriptivas.
            
            REGLAS CRÍTICAS:
            1. NUNCA alteres ni modifiques el título ni el contenido original de las notas.
            2. Clasifica cada nota en una sección (categoryName). Puedes reutilizar las secciones existentes del usuario si son adecuadas, o proponer nuevas secciones si es necesario (ej: "Personal", "Estudios", "Finanzas", "Trabajo", "Salud", etc.).
            3. Si una nota no encaja en ninguna sección específica, usa la categoría vacía "" para dejarla en la sección General.
            4. Responde SOLAMENTE con un JSON Array de objetos que contengan el ID de la nota ("noteId") y el nombre de la sección correspondiente ("categoryName").
            
            SECCIONES EXISTENTES DEL USUARIO (Priorízalas si son adecuadas):
            [$categoriesJson]

            INPUT DE NOTAS:
            [$notesJson]

            MODELO DE RESPUESTA JSON:
            [
              { "noteId": "ID_DE_LA_NOTA", "categoryName": "Estudios" }
            ]
        """.trimIndent()

        try {
            android.util.Log.d("AIService", "Organizing ${notes.size} notes with OpenCode...")
            
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", "Organiza mis notas por favor.")
            )

            val result = openCodeRepository.getChatCompletion(messages)
            val rawText = result.getOrNull() ?: "[]"
            val jsonText = rawText.extractJsonArray()
            
            val type = object : TypeToken<List<NoteOrganization>>() {}.type
            val organized: List<NoteOrganization> = gson.fromJson(jsonText, type) ?: emptyList()
            
            return@withContext organized
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Exception caught in organizeNotes", e)
            return@withContext emptyList()
        }
    }

    /**
     * Genera flashcards interactivas a partir del contenido de una nota, detectando términos difíciles
     * y anexando búsquedas automáticas a Wikipedia y YouTube.
     */
    suspend fun generateFlashcards(noteContent: String): List<com.katchy.focuslive.data.model.FlashcardItem> = withContext(Dispatchers.IO) {
        if (noteContent.isBlank()) return@withContext emptyList()
        val systemPrompt = """
            ERES UN EXPERTO EN EDUCACIÓN Y APRENDIZAJE ACTIVO (ACTIVE RECALL).
            Tu misión es analizar el texto provisto y generar una lista de tarjetas de memoria (Flashcards) interactivas (preguntas y respuestas) en español.
            
            REGLAS:
            1. Genera preguntas claras, concisas y directas sobre los conceptos más importantes.
            2. Genera respuestas precisas y fáciles de entender.
            3. Si detectas conceptos complejos, técnicos, científicos o históricos difíciles (ej: "Mitosis", "Teorema de Pitágoras", "Fotosíntesis"), identifica palabras clave específicas para buscar artículos explicativos en Wikipedia ("wikiKeyword") y videos cortos de explicación en YouTube ("youtubeKeyword"). De lo contrario, deja estos campos vacíos o null.
            4. Responde SOLAMENTE con un JSON Array válido. Sin bloques de código markdown, sin texto extra.
            
            MODELO DE RESPUESTA JSON:
            [
              {
                "question": "¿Cuál es la fase de la mitosis donde los cromosomas se alinean?",
                "answer": "La metafase.",
                "wikiKeyword": "Mitosis",
                "youtubeKeyword": "Mitosis explicacion rapida"
              }
            ]
        """.trimIndent()

        try {
            val messages = listOf(
                OpenCodeMessage("system", systemPrompt),
                OpenCodeMessage("user", noteContent)
            )
            val result = openCodeRepository.getChatCompletion(messages)
            val rawText = result.getOrNull() ?: "[]"
            val jsonText = rawText.extractJsonArray()
            
            data class AiFlashcardDto(
                val question: String,
                val answer: String,
                val wikiKeyword: String? = null,
                val youtubeKeyword: String? = null
            )
            
            val type = object : TypeToken<List<AiFlashcardDto>>() {}.type
            val dtos: List<AiFlashcardDto> = gson.fromJson(jsonText, type) ?: emptyList()
            
            return@withContext dtos.map { dto ->
                val wikiUrl = dto.wikiKeyword?.takeIf { it.isNotBlank() }?.let {
                    try {
                        "https://es.wikipedia.org/wiki/Special:Search?search=${java.net.URLEncoder.encode(it, "UTF-8")}"
                    } catch (e: Exception) {
                        null
                    }
                }
                val youtubeUrl = dto.youtubeKeyword?.takeIf { it.isNotBlank() }?.let {
                    try {
                        "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(it, "UTF-8")}"
                    } catch (e: Exception) {
                        null
                    }
                }
                com.katchy.focuslive.data.model.FlashcardItem(
                    question = dto.question,
                    answer = dto.answer,
                    box = 1,
                    nextReviewDate = System.currentTimeMillis(),
                    lastReviewDate = 0L,
                    sourceWikiUrl = wikiUrl,
                    sourceVideoUrl = youtubeUrl
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AIService", "Error generating flashcards", e)
            return@withContext emptyList()
        }
    }
}

data class PrioritizedTask(
    val taskId: String,
    val newPriority: String,
    val reason: String
)

data class NoteOrganization(
    val noteId: String,
    val categoryName: String
)
