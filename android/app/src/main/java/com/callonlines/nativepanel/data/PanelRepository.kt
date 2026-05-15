package com.callonlines.nativepanel.data

import com.callonlines.nativepanel.AppConfig
import retrofit2.HttpException

class PanelRepository(
    private val api: PanelApi,
    private val tokenStore: TokenStore,
) {
    private fun bearer(): String {
        val t = tokenStore.getToken() ?: error("No hay sesión")
        return "Bearer $t"
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> =
        withHttp {
            val u = username.trim().removePrefix("@").trim()
            val r = api.login(LoginRequest(u, password))
            if (!r.ok || r.token.isNullOrBlank()) {
                throw IllegalStateException(r.error ?: "Login fallido")
            }
            r
        }

    suspend fun loadMe(): Result<MeResponse> =
        withHttp {
            val r = api.me(bearer())
            if (!r.ok) throw IllegalStateException(r.error ?: "Error")
            r
        }

    suspend fun updateCallerId(value: String): Result<SimpleOkResponse> =
        withHttp {
            val r = api.updateCallerId(bearer(), CallerIdRequest(value.trim()))
            if (!r.ok) throw IllegalStateException(r.error ?: "Error")
            r
        }

    suspend fun changePassword(
        current: String,
        new: String,
        confirm: String,
    ): Result<PasswordChangeResponse> =
        withHttp {
            val r = api.changePassword(
                bearer(),
                PasswordRequest(
                    currentPassword = current,
                    newPassword = new,
                    confirmPassword = confirm,
                ),
            )
            if (!r.ok) throw IllegalStateException(r.error ?: "Error")
            r
        }

    private suspend fun <T> withHttp(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: HttpException) {
            val code = e.code()
            val raw = e.response()?.errorBody()?.string().orEmpty()
            val looksHtml =
                raw.contains("<!DOCTYPE", ignoreCase = true) ||
                    raw.contains("<html", ignoreCase = true) ||
                    raw.contains("<HTML", ignoreCase = true)
            val msg = when {
                code == 404 ->
                    "No se encontró la API (404). Sube server/api/ a la URL de API_BASE o corrige AppConfig.kt (actual: ${AppConfig.API_BASE})."
                looksHtml ->
                    "El servidor respondió con una página web (código $code), no con JSON. La URL de la API no es la correcta o falta index.php."
                raw.isNotBlank() && raw.length < 500 && '<' !in raw -> raw
                else -> raw.ifBlank { "HTTP $code" }
            }
            Result.failure(IllegalStateException(msg))
        } catch (e: Throwable) {
            Result.failure(e)
        }
}
