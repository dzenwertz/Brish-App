package com.katchy.focuslive.data.repository

import com.katchy.focuslive.data.api.OpenCodeApi
import com.katchy.focuslive.data.api.OpenCodeMessage
import com.katchy.focuslive.data.api.OpenCodeRequest
import com.katchy.focuslive.data.security.OpenCodeCredentialsStore
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

class PaywallException(message: String) : Exception(message)

interface OpenCodeRepository {
    suspend fun getChatCompletion(messages: List<OpenCodeMessage>): Result<String>
    suspend fun checkAccess(): Result<Boolean>
}

@Singleton
class OpenCodeRepositoryImpl @Inject constructor(
    private val openCodeApi: OpenCodeApi,
    private val credentialsStore: OpenCodeCredentialsStore
) : OpenCodeRepository {

    override suspend fun checkAccess(): Result<Boolean> = Result.success(credentialsStore.hasKey())

    override suspend fun getChatCompletion(messages: List<OpenCodeMessage>): Result<String> {
        val apiKey = credentialsStore.load()
            ?: return Result.failure(IllegalStateException("Agrega tu clave de OpenCode en Ajustes para usar Brish IA"))
        val candidateModels = listOf(
            "big-pickle",
            "mimo-v2.5-free",
            "ling-3.0-flash-fin-free",
            "nemotron-3.5-lightning-free"
        )
        var lastException: Exception? = null

        for (modelName in candidateModels) {
            val request = OpenCodeRequest(
                model = modelName,
                messages = messages,
                temperature = 0.4
            )

            try {
                val response = openCodeApi.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )
                val reply = cleanReply(response.choices?.firstOrNull()?.message?.content)
                if (!reply.isNullOrBlank()) return Result.success(reply)
            } catch (exception: Exception) {
                if (exception is kotlinx.coroutines.CancellationException) throw exception
                when ((exception as? HttpException)?.code()) {
                    401, 403 -> return Result.failure(
                        IllegalStateException("La clave de OpenCode no es válida o no tiene acceso")
                    )
                    402, 429 -> return Result.failure(
                        IllegalStateException("OpenCode no tiene saldo disponible o alcanzó su límite temporal")
                    )
                }
                lastException = exception
            }
        }

        return Result.failure(lastException ?: Exception("No se pudo obtener respuesta de Brish IA"))
    }

    private fun cleanReply(rawReply: String?): String? {
        if (rawReply.isNullOrBlank()) return null
        return rawReply.replace(Regex("(?s)<think>.*?</think>"), "").trim().ifBlank { null }
    }
}
