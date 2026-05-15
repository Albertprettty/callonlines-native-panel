package com.callonlines.nativepanel.data

import com.callonlines.nativepanel.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    fun panelApi(tokenStore: TokenStore): PanelApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                // No cerrar sesión en 401 de login (no lleva Bearer) ni en otros 401 sin cabecera de auth.
                if (response.code == 401) {
                    val auth = request.header("Authorization")
                    if (!auth.isNullOrBlank()) {
                        tokenStore.endSessionDueToUnauthorized()
                    }
                }
                response
            }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.API_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(PanelApi::class.java)
    }
}
