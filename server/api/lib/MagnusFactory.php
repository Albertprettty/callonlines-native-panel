<?php
declare(strict_types=1);

final class MagnusFactory
{
    /** @return object */
    public static function create(array $config)
    {
        $siteRoot = rtrim((string)$config['site_root'], '/');
        $mbFile = $siteRoot . '/vendor/magnussolution/magnusbilling-api/src/magnusBilling.php';

        if (!file_exists($mbFile)) {
            Json::out(500, ['ok' => false, 'error' => 'Magnus SDK no encontrado en: ' . $mbFile]);
        }

        require_once $mbFile;

        $apiKey = (string)$config['magnus_api_key'];
        $secret = (string)$config['magnus_secret_key'];
        $publicUrl = rtrim((string)$config['magnus_public_url'], '/');

        if ($apiKey === '' || $secret === '' || $publicUrl === '') {
            Json::out(500, ['ok' => false, 'error' => 'Falta configuración Magnus (API key, secret o public URL).']);
        }

        $classA = 'magnusbilling\\api\\MagnusBilling';
        $classB = 'magnusbilling\\api\\magnusBilling';
        $class = null;

        if (class_exists($classA)) {
            $class = $classA;
        } elseif (class_exists($classB)) {
            $class = $classB;
        } else {
            Json::out(500, ['ok' => false, 'error' => 'Clase MagnusBilling no encontrada.']);
        }

        /** @var object $mb */
        $mb = new $class($apiKey, $secret);
        $mb->public_url = $publicUrl;

        return $mb;
    }
}
