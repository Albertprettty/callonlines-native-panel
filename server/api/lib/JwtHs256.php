<?php
declare(strict_types=1);

final class JwtHs256
{
    private static function b64url(string $bin): string
    {
        return rtrim(strtr(base64_encode($bin), '+/', '-_'), '=');
    }

    private static function b64urlDecode(string $s): string
    {
        $pad = strlen($s) % 4;

        if ($pad > 0) {
            $s .= str_repeat('=', 4 - $pad);
        }

        $out = base64_decode(strtr($s, '-_', '+/'), true);

        return is_string($out) ? $out : '';
    }

    public static function encode(array $payload, string $secret, int $ttlSeconds): string
    {
        $now = time();
        $payload['iat'] = $now;
        $payload['exp'] = $now + $ttlSeconds;

        $header = ['typ' => 'JWT', 'alg' => 'HS256'];
        $h = self::b64url(json_encode($header, JSON_UNESCAPED_UNICODE));
        $p = self::b64url(json_encode($payload, JSON_UNESCAPED_UNICODE));
        $sig = hash_hmac('sha256', $h . '.' . $p, $secret, true);

        return $h . '.' . $p . '.' . self::b64url($sig);
    }

    /** @return array<string,mixed>|null */
    public static function decode(string $jwt, string $secret): ?array
    {
        $parts = explode('.', $jwt);

        if (count($parts) !== 3) {
            return null;
        }

        [$h, $p, $s] = $parts;
        $check = hash_hmac('sha256', $h . '.' . $p, $secret, true);

        if (!hash_equals(self::b64url($check), $s)) {
            return null;
        }

        $json = self::b64urlDecode($p);
        $data = json_decode($json, true);

        if (!is_array($data)) {
            return null;
        }

        if (!isset($data['exp']) || time() >= (int)$data['exp']) {
            return null;
        }

        return $data;
    }
}
