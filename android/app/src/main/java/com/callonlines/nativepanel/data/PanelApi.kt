package com.callonlines.nativepanel.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface PanelApi {
    @POST("index.php?action=login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("index.php?action=me")
    suspend fun me(@Header("Authorization") authorization: String): MeResponse

    @POST("index.php?action=callerid")
    suspend fun updateCallerId(
        @Header("Authorization") authorization: String,
        @Body body: CallerIdRequest,
    ): SimpleOkResponse

    @POST("index.php?action=password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body body: PasswordRequest,
    ): PasswordChangeResponse
}
