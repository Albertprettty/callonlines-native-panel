package com.callonlines.nativepanel

/**
 * URL base de la carpeta donde está index.php de la API (con barra final).
 * Debe coincidir con donde subiste `server/api/` en tu hosting.
 *
 * Por defecto: CallOnLines. Si usas otra ruta, cámbiala y vuelve a generar la APK.
 */
object AppConfig {
    const val API_BASE: String = "https://callonlines.com/app_api/"
}
