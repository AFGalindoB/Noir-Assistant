package com.afgalindob.assistantapp.data.repository.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.local.preferences.UserPreferencesManager
import com.afgalindob.assistantapp.data.repository.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private const val tag = "NetworkRepository"
class NetworkRepositoryImpl(
    private val preferencesManager: UserPreferencesManager,
    private val httpClient: OkHttpClient
) : NetworkRepository {

    override suspend fun processQrAuthData(qrJson: String): Result<Triple<String, String, String>> {
        return try {
            val json = JSONObject(qrJson)

            if (json.optString("header") != "noir-app-v2.0.0") {
                Log.e(tag, "QR Rechazado: Header inválido")
                return Result.failure(Exception("Origen desconocido: Header inválido"))
            }

            val url = json.getString("base_url")
            val sessionId = json.getString("sid")
            val endpoint = json.getString("endpoint")
            val qrType = json.getString("qr_type")

            preferencesManager.clearToken()
            preferencesManager.saveUrl(url)

            Log.d(tag, "Configuración guardada correctamente. Url: $url")
            Result.success(Triple(endpoint, sessionId, qrType))
        } catch (e: Exception) {
            Log.e(tag, "Error al parsear JSON del QR: ${e.message}")
            Result.failure(Exception("QR incompatible con el protocolo de Noir"))
        }
    }

    override suspend fun processQrTokenData(qrJson: String): Result<Pair<String, String>> {
        return try {
            val json = JSONObject(qrJson)

            if (json.optString("header") != "noir-app-v2.0.0") {
                Log.e(tag, "QR Rechazado: Header inválido")
                return Result.failure(Exception("Origen desconocido: Header inválido"))
            }

            val endpoint = json.getString("endpoint")
            val qrType = json.getString("qr_type")

            Log.d(tag, "QR procesado correctamente.")
            Result.success(Pair(endpoint, qrType))
        } catch (e: Exception) {
            Log.e(tag, "Error al parsear JSON del QR: ${e.message}")
            Result.failure(Exception("QR incompatible con el protocolo de Noir"))
        }
    }

    override suspend fun checkServerHealth(): Boolean {
        val baseUrl = preferencesManager.getUrlSynchronous() ?: return false

        val url = "$baseUrl/health".toHttpUrlOrNull() ?: run {
            Log.e(tag, "Health Check cancelado: URL inválida -> $baseUrl/health")
            return false
        }

        Log.d(tag, "Verificando salud del servidor en: $url")

        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val shortClient = httpClient.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()

            shortClient.newCall(request).execute().use { response ->
                Log.d(tag, "Health Check -> Code: ${response.code}, Success: ${response.isSuccessful}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Fallo crítico en Health Check: ${e.message}")
            false
        }
    }

    override suspend fun checkCredentials(): Pair<NetworkResult, String> {
        val baseUrl = preferencesManager.getUrlSynchronous() ?: return NetworkResult.CONNECTIVITY_ERROR to "URL no configurada"
        val token = preferencesManager.getTokenSynchronous() ?: return NetworkResult.LOGIC_ERROR to "No se encontró un token de sesión almacenado"
        val username = preferencesManager.getServerUsernameSynchronous() ?: return NetworkResult.LOGIC_ERROR to "No se encontró un nombre de usuario almacenado"

        val deviceId = android.os.Build.ID

        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegments("auth/validate_credentials")
            ?.addQueryParameter("device_id", deviceId)
            ?.addQueryParameter("username", username)
            ?.build() ?: return NetworkResult.LOGIC_ERROR to "La URL de dominio configurada no es válida"

        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                Log.d(tag, "Token Validation Result: ${response.code} - $bodyString")

                when (response.code) {
                    200 -> NetworkResult.SUCCESS to "Sesión válida"
                    401 -> {
                        preferencesManager.clearToken()
                        NetworkResult.LOGIC_ERROR to "La sesión ha expirado o es inválida"
                    }
                    403 -> {
                        preferencesManager.clearToken()
                        NetworkResult.LOGIC_ERROR to "El acceso ha sido revocado por el administrador"
                    }
                    404 -> {
                        preferencesManager.clearToken()
                        NetworkResult.LOGIC_ERROR to "El dispositivo no está registrado en el servidor"
                    }
                    else -> NetworkResult.LOGIC_ERROR to "Problema de respuesta del servidor. CODE: ${response.code}"
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error de red al validar token: ${e.message}")
            NetworkResult.CONNECTIVITY_ERROR to "No se pudo conectar con el servidor"
        }
    }

    override suspend fun requestAuth(endpoint: String, sessionId: String, typeQr: String): Pair<NetworkResult, String> {
        val baseUrl = preferencesManager.getUrlSynchronous() ?: return NetworkResult.CONNECTIVITY_ERROR to "URL no configurada"

        val fullUrl = "$baseUrl$endpoint".toHttpUrlOrNull()
            ?: return NetworkResult.LOGIC_ERROR to "Ruta final inválida o mal estructurada"

        Log.d(tag, "Base URL: $baseUrl")
        Log.d(tag, "Endpoint Path: $endpoint")
        Log.d(tag, "Iniciando Request Auth en: $fullUrl")

        val jsonBody = JSONObject().apply {
            put("sid", sessionId)
            put("type", typeQr)
            put("device_id",  android.os.Build.ID)
            put("device_name", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }

        val request = Request.Builder()
            .url(fullUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                Log.d(tag, "Auth Request Result: ${response.code} - $bodyString")

                when (response.code) {
                    200 -> NetworkResult.SUCCESS to "Solicitud enviada. Espera la aprobación en el panel."
                    202 -> NetworkResult.SUCCESS to "Dispositivo reactivado. Escanea el QR de acceso."
                    400 -> NetworkResult.LOGIC_ERROR to "El código QR no es válido para esta operación."
                    403 -> {
                        preferencesManager.clearToken()
                        NetworkResult.LOGIC_ERROR to "Este hardware ha sido baneado del sistema."
                    }
                    410 -> {
                        NetworkResult.LOGIC_ERROR to "El QR ha expirado o ya fue reclamado por otro dispositivo."
                    }
                    else -> NetworkResult.LOGIC_ERROR to "Error en la solicitud. CODE: ${response.code}."
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error de conexión en Request Auth : ${e.message}")
            NetworkResult.CONNECTIVITY_ERROR to "No se pudo alcanzar el servidor. Verifica la conexión."
        }
    }

    override suspend fun requestToken(endpoint: String, typeQr: String): Pair<NetworkResult, String> {
        val baseUrl = preferencesManager.getUrlSynchronous() ?: return NetworkResult.CONNECTIVITY_ERROR to "URL no configurada"
        val username = preferencesManager.getServerUsernameSynchronous() ?: return NetworkResult.LOGIC_ERROR to "No se encontró un nombre de usuario almacenado"

        val fullUrl = "$baseUrl$endpoint".toHttpUrlOrNull()
            ?: return NetworkResult.LOGIC_ERROR to "La ruta del endpoint solicitada es inválida"

        Log.d(tag, "Base URL: $baseUrl")
        Log.d(tag, "Endpoint Path: $endpoint")
        Log.d(tag, "Iniciando Request Token en: $fullUrl")

        val jsonBody = JSONObject().apply {
            put("username", username)
            put("type", typeQr)
            put("device_id", android.os.Build.ID)
            put("device_name", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }

        val request = Request.Builder()
            .url(fullUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val jsonResponse = JSONObject(body)
                Log.d(tag, "Request Token Result -> Code: ${response.code}, Body: $body")

                if (response.isSuccessful) {
                    preferencesManager.saveToken(jsonResponse.getString("token"))
                    NetworkResult.SUCCESS to "Token de autenticación obtenido y guardado."
                } else {
                    val userFriendlyMessage = when (response.code) {
                        401 -> NetworkResult.LOGIC_ERROR to "Credenciales incorrectas o el administrador no ha aprobado este dispositivo"
                        403 -> NetworkResult.LOGIC_ERROR to "Este dispositivo tiene el acceso restringido"
                        422 -> NetworkResult.LOGIC_ERROR to "El código QR no es válido para esta operación."
                        else -> NetworkResult.LOGIC_ERROR to "Error en la solicitud. CODE: ${response.code}"
                    }
                    userFriendlyMessage
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error de red: ${e.message}")
            NetworkResult.CONNECTIVITY_ERROR to "Sin respuesta del servidor. Inténtalo más tarde."
        }
    }

    override suspend fun uploadVoiceAudio(file: File): Pair<NetworkResult, String> {
        val baseUrl = preferencesManager.getUrlSynchronous() ?: return NetworkResult.CONNECTIVITY_ERROR to "URL no configurada"
        val token = preferencesManager.getTokenSynchronous() ?: return NetworkResult.LOGIC_ERROR to "No se encontró un token de sesión almacenado"
        val username = preferencesManager.getServerUsernameSynchronous() ?: return NetworkResult.LOGIC_ERROR to "No se encontró un nombre de usuario almacenado"
        val deviceId = android.os.Build.ID

        val fullUrl = "$baseUrl/ia/process_audio".toHttpUrlOrNull()
            ?: return NetworkResult.LOGIC_ERROR to "Estructura de red corrupta para el procesamiento de IA"

        Log.d(tag, "Iniciando subida de audio multipart a: $fullUrl")

        val mediaType = "audio/m4a".toMediaType()
        val fileRequestBody = file.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("device_id", deviceId)
            .addFormDataPart("username", username)
            .addFormDataPart("token", token)
            .addFormDataPart(
                "audio_file",
                file.name,
                fileRequestBody
            )
            .build()

        val request = Request.Builder()
            .url(fullUrl)
            .post(requestBody)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                Log.d(tag, "Upload Audio Result -> Code: ${response.code}, Body: $bodyString")

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(bodyString)
                    if (jsonResponse.optString("status") == "accepted") {
                        NetworkResult.SUCCESS to "Audio recibido y enviado a procesamiento."
                    } else {
                        NetworkResult.LOGIC_ERROR to "Respuesta inesperada del servidor de Milo. CODE: ${response.code}"
                    }
                } else {
                    when (response.code) {
                        400 -> NetworkResult.LOGIC_ERROR to "Formato de audio no soportado por la IA (debe ser .m4a)"
                        401 -> NetworkResult.LOGIC_ERROR to "Credenciales inválidas. El token fue rechazado por la capa de seguridad."
                        403 -> NetworkResult.LOGIC_ERROR to "Acceso denegado. El dispositivo está desactivado o baneado en el servidor."
                        404 -> NetworkResult.LOGIC_ERROR to "Dispositivo no registrado en el sistema central."
                        else -> NetworkResult.LOGIC_ERROR to "Error en el procesamiento del audio. CODE: ${response.code}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error de red al enviar audio a Milo: ${e.message}")
            NetworkResult.CONNECTIVITY_ERROR to "No se pudo conectar con el servidor. Intentando más tarde."
        }
    }

    override suspend fun getProcessedAudios(): Triple<NetworkResult, String, String> = withContext(Dispatchers.IO) {
        val baseUrl = preferencesManager.getUrlSynchronous() ?: return@withContext Triple(NetworkResult.CONNECTIVITY_ERROR, "URL no configurada", "")
        val token = preferencesManager.getTokenSynchronous() ?: return@withContext Triple(NetworkResult.LOGIC_ERROR, "No se encontró un token de sesión almacenado", "")
        val username = preferencesManager.getServerUsernameSynchronous() ?: return@withContext Triple(NetworkResult.LOGIC_ERROR, "No se encontró un nombre de usuario almacenado", "")
        val deviceId = android.os.Build.ID

        val url = "$baseUrl/ia/processed_audios"
        val urlBuilder = url.toHttpUrlOrNull()?.newBuilder()
            ?: return@withContext Triple(NetworkResult.LOGIC_ERROR, "URL base inválida: $url", "")

        Log.d(tag, "Iniciando consulta de audios procesados a: $url")

        val fullUrl = urlBuilder
            .addQueryParameter("device_id", deviceId)
            .addQueryParameter("username", username)
            .build()

        Log.d(tag, "Solicitando JSONs procesados a: $fullUrl")

        val request = Request.Builder()
            .url(fullUrl)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return@withContext try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                Log.d(tag, "Get Processed Audio Result -> Code: ${response.code}, Body: $bodyString")

                if (response.isSuccessful) {
                    Triple(NetworkResult.SUCCESS, "Audios procesados recuperados con éxito.", bodyString)
                } else {
                    val errorMsg = when (response.code) {
                        401 -> "Credenciales inválidas. Sesión expirada o token rechazado."
                        403 -> "Acceso denegado. El dispositivo está desactivado o baneado en el servidor."
                        404 -> "El dispositivo no se encuentra registrado en el sistema."
                        else -> "Error en el servidor al recuperar transcripciones. CODE: ${response.code}"
                    }
                    Triple(NetworkResult.LOGIC_ERROR, errorMsg, bodyString)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error de red al consultar audios procesados: ${e.message}")
            Triple(NetworkResult.CONNECTIVITY_ERROR, "Error de red. No se pudo conectar con el servidor.", "")
        }

    }
}