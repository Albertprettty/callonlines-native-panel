<?php
declare(strict_types=1);

final class RateLimit
{
    public static function clientIp(): string
    {
        if (!empty($_SERVER['HTTP_CF_CONNECTING_IP'])) {
            return (string)$_SERVER['HTTP_CF_CONNECTING_IP'];
        }

        if (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
            return trim(explode(',', (string)$_SERVER['HTTP_X_FORWARDED_FOR'])[0]);
        }

        $ip = $_SERVER['REMOTE_ADDR'] ?? '';

        return is_string($ip) ? $ip : '';
    }

    public static function ipOk(string $key, string $ip, int $limit, int $windowSeconds): bool
    {
        $ip = trim($ip);

        if ($ip === '') {
            return true;
        }

        $now = time();
        $safeKey = preg_replace('/[^a-zA-Z0-9_\-]/', '_', $key) ?: 'k';
        $hash = hash('sha256', $ip . '|' . $safeKey);
        $file = rtrim(sys_get_temp_dir(), DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . "co_api_rl_{$safeKey}_{$hash}.json";

        $data = ['hits' => []];
        $fp = @fopen($file, 'c+');

        if ($fp === false) {
            return true;
        }

        if (!flock($fp, LOCK_EX)) {
            fclose($fp);

            return true;
        }

        $size = @filesize($file);

        if ($size && $size > 0) {
            rewind($fp);
            $raw = (string)stream_get_contents($fp);
            $decoded = json_decode($raw ?: '', true);

            if (is_array($decoded) && isset($decoded['hits']) && is_array($decoded['hits'])) {
                $data = $decoded;
            }
        }

        $data['hits'] = array_values(array_filter(
            $data['hits'],
            static function ($ts) use ($now, $windowSeconds) {
                return (is_int($ts) || ctype_digit((string)$ts)) && ($now - (int)$ts) < $windowSeconds;
            }
        ));

        if (count($data['hits']) >= $limit) {
            flock($fp, LOCK_UN);
            fclose($fp);

            return false;
        }

        $data['hits'][] = $now;
        ftruncate($fp, 0);
        rewind($fp);
        fwrite($fp, json_encode($data));
        fflush($fp);
        flock($fp, LOCK_UN);
        fclose($fp);

        return true;
    }
}
