<?php
/**
 * Copia este archivo como config.php y ajusta rutas/claves.
 * config.php no debe subirse a git con secretos reales.
 */
declare(strict_types=1);

return [
    // Carpeta del sitio: donde está vendor/ y panel_cliente.php.
    // Si subes esta API a public_html/app_api/, deja dirname(__DIR__) (= public_html).
    'site_root' => dirname(__DIR__),

    'jwt_secret' => getenv('CALLONLINES_JWT_SECRET') ?: 'CAMBIA_ESTO_POR_UNA_CADENA_LARGA_ALEATORIA',
    'jwt_ttl_seconds' => 8 * 3600,

    'magnus_api_key' => getenv('CALLONLINES_MAGNUS_API_KEY') ?: '',
    'magnus_secret_key' => getenv('CALLONLINES_MAGNUS_SECRET_KEY') ?: '',
    'magnus_public_url' => getenv('CALLONLINES_MAGNUS_PUBLIC_URL') ?: '',

    'sip_server' => getenv('CALLONLINES_SIP_HOST') ?: 'sip.callonlines.com',
    'sip_port' => getenv('CALLONLINES_SIP_PORT') ?: '5060',

    'low_balance_threshold' => 5.0,
    'recharge_page_url' => 'https://callonlines.com/recarga.php',
    'recharge_wa_number' => '18293219420',

    // Rate limit cambio de contraseña (misma idea que panel_cliente.php)
    'pass_rl_session_limit' => 5,
    'pass_rl_session_window' => 600,
    'pass_rl_ip_limit' => 10,
    'pass_rl_ip_window' => 900,

    'login_rl_ip_limit' => 20,
    'login_rl_ip_window' => 900,
];
