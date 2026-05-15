<?php
/**
 * CallOnLines — API JSON para app nativa.
 * Sube la carpeta api/ junto a panel_cliente.php (mismo nivel que vendor/).
 *
 * Endpoints (JSON):
 *   POST ?action=login     { "username", "password" }
 *   GET  ?action=me        Header: Authorization: Bearer <jwt>
 *   POST ?action=callerid  { "callerid" }
 *   POST ?action=password  { "current_password", "new_password", "confirm_password" }  (new vacío = auto)
 */
declare(strict_types=1);

header('X-Content-Type-Options: nosniff');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

require_once __DIR__ . '/lib/Json.php';
require_once __DIR__ . '/lib/JwtHs256.php';
require_once __DIR__ . '/lib/PasswordUtil.php';
require_once __DIR__ . '/lib/RateLimit.php';
require_once __DIR__ . '/lib/MagnusFactory.php';

$configPath = __DIR__ . '/config.php';

if (!file_exists($configPath)) {
    Json::out(500, ['ok' => false, 'error' => 'Falta config.php (copia config.sample.php).']);
}

/** @var array<string,mixed> $config */
$config = require $configPath;

$action = isset($_GET['action']) ? (string)$_GET['action'] : '';
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

/** @param object $mb */
function co_find_sip_for_user($mb, int $idUser): ?array
{
    $mb->clearFilter();
    $mb->setFilter('id_user', $idUser, 'eq', 'numeric');
    $sipResult = $mb->read('sip', 1);

    if (isset($sipResult['rows'][0]) && is_array($sipResult['rows'][0])) {
        return $sipResult['rows'][0];
    }

    return null;
}

/** @param object $mb */
function co_is_api_error($res): bool
{
    return is_array($res) && (
        (isset($res['status']) && $res['status'] === 'error') ||
        (isset($res['success']) && $res['success'] === false) ||
        isset($res['error']) ||
        (isset($res['errors']) && $res['errors'])
    );
}

function co_bearer_token(): string
{
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['Authorization'] ?? '';

    if (!is_string($h) || $h === '') {
        return '';
    }

    if (preg_match('/Bearer\s+(\S+)/i', $h, $m)) {
        return (string)$m[1];
    }

    return '';
}

function co_display_handle(string $usernameNorm): string
{
    return str_starts_with($usernameNorm, '@') ? $usernameNorm : '@' . $usernameNorm;
}

// ---------- LOGIN ----------
if ($action === 'login' && $method === 'POST') {
    $ip = RateLimit::clientIp();

    if (!RateLimit::ipOk('api_login', $ip, (int)$config['login_rl_ip_limit'], (int)$config['login_rl_ip_window'])) {
        Json::out(429, ['ok' => false, 'error' => 'Demasiados intentos. Espera e intenta de nuevo.']);
    }

    $body = Json::readBody();
    $usernameRaw = trim((string)($body['username'] ?? ''));
    $password = (string)($body['password'] ?? '');

    if ($usernameRaw === '' || $password === '') {
        Json::out(400, ['ok' => false, 'error' => 'Usuario y contraseña son obligatorios.']);
    }

    $usernameNorm = PasswordUtil::normalizeUsername($usernameRaw);
    $mb = MagnusFactory::create($config);

    try {
        $idUser = $mb->getId('user', 'username', $usernameNorm);
    } catch (Throwable $e) {
        Json::out(502, ['ok' => false, 'error' => 'Error Magnus: ' . $e->getMessage()]);
    }

    if (!$idUser) {
        Json::out(401, ['ok' => false, 'error' => 'Credenciales incorrectas.']);
    }

    $mb->setFilter('id', $idUser, 'eq', 'numeric');
    $result = $mb->read('user', 1);

    if (!isset($result['rows'][0]) || !is_array($result['rows'][0])) {
        Json::out(401, ['ok' => false, 'error' => 'Credenciales incorrectas.']);
    }

    $userData = $result['rows'][0];
    $stored = (string)($userData['password'] ?? '');

    if (!PasswordUtil::passwordMatchesStored($password, $stored)) {
        Json::out(401, ['ok' => false, 'error' => 'Credenciales incorrectas.']);
    }

    $jwtSecret = (string)$config['jwt_secret'];

    if ($jwtSecret === '' || $jwtSecret === 'CAMBIA_ESTO_POR_UNA_CADENA_LARGA_ALEATORIA') {
        Json::out(500, ['ok' => false, 'error' => 'jwt_secret inseguro o vacío en config.php']);
    }

    $ttl = (int)$config['jwt_ttl_seconds'];
    $token = JwtHs256::encode(['sub' => $usernameNorm], $jwtSecret, $ttl);

    Json::out(200, [
        'ok' => true,
        'token' => $token,
        'token_type' => 'Bearer',
        'expires_in' => $ttl,
        'user' => [
            'username' => $usernameNorm,
            'display' => co_display_handle($usernameNorm),
        ],
    ]);
}

// ---------- AUTH for rest ----------
$token = co_bearer_token();

if ($token === '') {
    Json::out(401, ['ok' => false, 'error' => 'Falta Authorization: Bearer']);
}

$jwtSecret = (string)$config['jwt_secret'];
$claims = JwtHs256::decode($token, $jwtSecret);

if ($claims === null || empty($claims['sub']) || !is_string($claims['sub'])) {
    Json::out(401, ['ok' => false, 'error' => 'Token inválido o expirado.']);
}

$usernameNorm = PasswordUtil::normalizeUsername($claims['sub']);
$mb = MagnusFactory::create($config);

// ---------- ME ----------
if ($action === 'me' && $method === 'GET') {
    try {
        $idUser = $mb->getId('user', 'username', $usernameNorm);
    } catch (Throwable $e) {
        Json::out(502, ['ok' => false, 'error' => 'Error Magnus: ' . $e->getMessage()]);
    }

    if (!$idUser) {
        Json::out(404, ['ok' => false, 'error' => 'Usuario no encontrado.']);
    }

    $mb->setFilter('id', $idUser, 'eq', 'numeric');
    $result = $mb->read('user', 1);

    if (!isset($result['rows'][0]) || !is_array($result['rows'][0])) {
        Json::out(404, ['ok' => false, 'error' => 'No se pudo leer el usuario.']);
    }

    $userData = $result['rows'][0];
    $saldo = $userData['credit'] ?? 0;

    $mb->clearFilter();
    $sip = co_find_sip_for_user($mb, (int)$idUser);
    $callerId = '';

    if ($sip) {
        $callerId = (string)($sip['callerid'] ?? '');
    }

    $lang = 'es';
    $saldoTxt = number_format((float)$saldo, 2, '.', '');
    $waText = "Hola, quiero recargar mi cuenta.\nUsuario: {$usernameNorm}\nSaldo: {$saldoTxt} USD";
    $waNum = (string)$config['recharge_wa_number'];
    $waUrl = 'https://wa.me/' . $waNum . '?text=' . rawurlencode($waText);

    $low = (float)$saldo <= (float)$config['low_balance_threshold'];

    Json::out(200, [
        'ok' => true,
        'user' => [
            'username' => $usernameNorm,
            'display' => co_display_handle($usernameNorm),
        ],
        'balance' => (float)$saldo,
        'balance_formatted' => $saldoTxt,
        'low_balance' => $low,
        'sip' => [
            'username' => $usernameNorm,
            'server' => (string)$config['sip_server'],
            'port' => (string)$config['sip_port'],
            'callerid' => $callerId,
        ],
        'links' => [
            'recharge_web' => (string)$config['recharge_page_url'],
            'recharge_whatsapp' => $waUrl,
        ],
        'lang' => $lang,
    ]);
}

// ---------- CALLERID ----------
if ($action === 'callerid' && $method === 'POST') {
    $body = Json::readBody();
    $nuevo = trim((string)($body['callerid'] ?? ''));

    if ($nuevo === '') {
        Json::out(400, ['ok' => false, 'error' => 'CallerID vacío.']);
    }

    if (!PasswordUtil::callerIdIsValid($nuevo)) {
        Json::out(400, ['ok' => false, 'error' => 'CallerID inválido (6–16 dígitos, opcional + al inicio).']);
    }

    try {
        $idUser = $mb->getId('user', 'username', $usernameNorm);
    } catch (Throwable $e) {
        Json::out(502, ['ok' => false, 'error' => 'Error Magnus: ' . $e->getMessage()]);
    }

    if (!$idUser) {
        Json::out(404, ['ok' => false, 'error' => 'Usuario no encontrado.']);
    }

    $sip = co_find_sip_for_user($mb, (int)$idUser);

    if (!$sip || empty($sip['id'])) {
        Json::out(400, ['ok' => false, 'error' => 'No hay registro SIP para este usuario.']);
    }

    $idSip = (int)$sip['id'];

    try {
        $up = $mb->update('sip', $idSip, ['callerid' => $nuevo]);
    } catch (Throwable $e) {
        Json::out(502, ['ok' => false, 'error' => 'Error al actualizar: ' . $e->getMessage()]);
    }

    if (co_is_api_error($up)) {
        Json::out(502, ['ok' => false, 'error' => 'Magnus rechazó la actualización.', 'detail' => $up]);
    }

    Json::out(200, ['ok' => true, 'callerid' => $nuevo]);
}

// ---------- PASSWORD ----------
if ($action === 'password' && $method === 'POST') {
    $ip = RateLimit::clientIp();

    if (!RateLimit::ipOk('api_pass_' . $usernameNorm, $ip, (int)$config['pass_rl_ip_limit'], (int)$config['pass_rl_ip_window'])) {
        Json::out(429, ['ok' => false, 'error' => 'Demasiados intentos de cambio de contraseña. Espera unos minutos.']);
    }

    $body = Json::readBody();
    $cur = trim((string)($body['current_password'] ?? ''));
    $new = trim((string)($body['new_password'] ?? ''));
    $confirm = trim((string)($body['confirm_password'] ?? ''));

    if ($cur === '') {
        Json::out(400, ['ok' => false, 'error' => 'Contraseña actual obligatoria.']);
    }

    $generated = false;

    if ($new === '') {
        $new = PasswordUtil::generatePassword(12);
        $confirm = $new;
        $generated = true;
    }

    if ($new !== $confirm) {
        Json::out(400, ['ok' => false, 'error' => 'La nueva contraseña y la confirmación no coinciden.']);
    }

    if (!PasswordUtil::passwordIsValid($new)) {
        Json::out(400, ['ok' => false, 'error' => 'Contraseña inválida (8–32 caracteres, sin espacios).']);
    }

    try {
        $idUser = $mb->getId('user', 'username', $usernameNorm);
    } catch (Throwable $e) {
        Json::out(502, ['ok' => false, 'error' => 'Error Magnus: ' . $e->getMessage()]);
    }

    if (!$idUser) {
        Json::out(404, ['ok' => false, 'error' => 'Usuario no encontrado.']);
    }

    $mb->setFilter('id', $idUser, 'eq', 'numeric');
    $result = $mb->read('user', 1);

    if (!isset($result['rows'][0]) || !is_array($result['rows'][0])) {
        Json::out(404, ['ok' => false, 'error' => 'No se pudo leer el usuario.']);
    }

    $stored = (string)($result['rows'][0]['password'] ?? '');

    if (!PasswordUtil::passwordMatchesStored($cur, $stored)) {
        // 400: error de datos; 401 queda reservado a token JWT inválido (la app cierra sesión ante 401).
        Json::out(400, ['ok' => false, 'error' => 'La contraseña actual es incorrecta.']);
    }

    $sip = co_find_sip_for_user($mb, (int)$idUser);

    if (!$sip || empty($sip['id'])) {
        Json::out(400, ['ok' => false, 'error' => 'No hay registro SIP para este usuario.']);
    }

    $idSip = (int)$sip['id'];

    try {
        $up1 = $mb->update('user', (int)$idUser, ['password' => $new]);
        $up2 = $mb->update('sip', $idSip, ['secret' => $new]);
    } catch (Throwable $e) {
        Json::out(502, ['ok' => false, 'error' => 'Error al actualizar: ' . $e->getMessage()]);
    }

    if (co_is_api_error($up1) || co_is_api_error($up2)) {
        Json::out(502, ['ok' => false, 'error' => 'Magnus rechazó el cambio.', 'detail' => ['user' => $up1, 'sip' => $up2]]);
    }

    $payload = ['ok' => true, 'must_login_again' => true];

    if ($generated) {
        $payload['generated_password'] = $new;
    }

    Json::out(200, $payload);
}

Json::out(404, ['ok' => false, 'error' => 'Acción no encontrada. Usa ?action=login|me|callerid|password']);
