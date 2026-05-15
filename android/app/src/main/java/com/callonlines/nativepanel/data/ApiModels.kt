package com.callonlines.nativepanel.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val ok: Boolean,
    val token: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    val user: UserBrief? = null,
    val error: String? = null,
)

data class UserBrief(
    val username: String? = null,
    val display: String? = null,
)

data class MeResponse(
    val ok: Boolean,
    val error: String? = null,
    val balance: Double? = null,
    @SerializedName("balance_formatted") val balanceFormatted: String? = null,
    @SerializedName("low_balance") val lowBalance: Boolean? = null,
    val user: UserBrief? = null,
    val sip: SipInfo? = null,
    val links: LinksInfo? = null,
)

data class SipInfo(
    val username: String? = null,
    val server: String? = null,
    val port: String? = null,
    val callerid: String? = null,
)

data class LinksInfo(
    @SerializedName("recharge_web") val rechargeWeb: String? = null,
    @SerializedName("recharge_whatsapp") val rechargeWhatsapp: String? = null,
)

data class CallerIdRequest(val callerid: String)

data class PasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String,
)

data class SimpleOkResponse(
    val ok: Boolean,
    val error: String? = null,
    val callerid: String? = null,
)

data class PasswordChangeResponse(
    val ok: Boolean,
    val error: String? = null,
    @SerializedName("must_login_again") val mustLoginAgain: Boolean? = null,
    @SerializedName("generated_password") val generatedPassword: String? = null,
)
