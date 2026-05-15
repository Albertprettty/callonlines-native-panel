<?php
declare(strict_types=1);

if (!function_exists('str_starts_with')) {
    /**
     * @phpstan-ignore-next-line
     */
    function str_starts_with(string $haystack, string $needle): bool
    {
        return $needle === '' || strpos($haystack, $needle) === 0;
    }
}

final class PasswordUtil
{
    public static function normalizeUsername(string $username): string
    {
        $u = trim($username);

        return ltrim($u, '@');
    }

    public static function passwordIsValid(string $p): bool
    {
        $p = trim($p);

        if ($p === '') {
            return false;
        }

        if (strlen($p) < 8 || strlen($p) > 32) {
            return false;
        }

        return !preg_match('/\s/', $p);
    }

    public static function callerIdIsValid(string $v): bool
    {
        $v = trim($v);

        return (bool)preg_match('/^\+?[0-9]{6,16}$/', $v);
    }

    public static function passwordMatchesStored(string $input, string $stored): bool
    {
        $storedTrim = trim($stored);

        if ($storedTrim === '') {
            return false;
        }

        if (
            str_starts_with($storedTrim, '$2y$') ||
            str_starts_with($storedTrim, '$2a$') ||
            str_starts_with($storedTrim, '$2b$') ||
            str_starts_with($storedTrim, '$argon2')
        ) {
            return password_verify($input, $storedTrim);
        }

        if (hash_equals($storedTrim, $input)) {
            return true;
        }

        if (hash_equals($storedTrim, md5($input))) {
            return true;
        }

        if (hash_equals($storedTrim, sha1($input))) {
            return true;
        }

        return false;
    }

    public static function generatePassword(int $len = 12): string
    {
        $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%';
        $max = strlen($alphabet) - 1;
        $out = '';

        for ($i = 0; $i < $len; $i++) {
            $out .= $alphabet[random_int(0, $max)];
        }

        return $out;
    }
}
