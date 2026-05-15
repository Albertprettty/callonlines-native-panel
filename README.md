# CallOnLines — Panel cliente nativo + API JSON

## APK lista para instalar (sin Android Studio en tu Mac)

El Mac no tenía Java para compilar aquí; en su lugar el repo incluye **GitHub Actions**: cada `push` a `main` genera una **APK debug** que puedes bajar.

1. Sube los últimos cambios a GitHub:  
   `cd ~/Desktop/callonlines-native-panel && git add -A && git commit -m "..." && git push`
2. En el repo: pestaña **Actions** → workflow **Build Android APK** → abre la ejecución verde.
3. En **Artifacts**, descarga **`callonlines-panel-debug`** (es un zip con `app-debug.apk`).
4. Pasa el APK al teléfono e instálalo (en Android: permitir “fuentes desconocidas” o instalar por ADB).

La APK **solo funcionará** si:

- Subiste la API (`server/api/`) a la URL que pusiste en `AppConfig.kt` (por defecto `https://callonlines.com/app_api/`).
- Existe `config.php` en el servidor con Magnus + `jwt_secret` bien configurados.

Para otra URL de API, cambia `API_BASE` en `android/.../AppConfig.kt`, haz commit y push, y vuelve a descargar el artifact.

---

App **Android** (Kotlin + Compose) que pide **usuario y contraseña** y luego muestra las funciones del **panel cliente** (saldo, CallerID, SIP, recargas, cambio de clave). La app habla con una **API JSON en PHP** que tú subes al mismo servidor donde está Magnus (`vendor/`).

## Estructura

- `server/api/` — API (`index.php`, `lib/`, `config.sample.php`). Copia `config.sample.php` → `config.php` y configura claves (no subas `config.php` con secretos a sitios públicos sin cuidado).
- `android/` — Proyecto Android Studio.
- `server/INSTRUCCIONES.txt` — Despliegue de la API.

## Configuración rápida

1. Sube `server/api/` a tu hosting (ej. `https://tudominio.com/app_api/` junto a `vendor/`).
2. Crea `config.php` desde el sample y define `CALLONLINES_JWT_SECRET`, Magnus API, etc.
3. En `android/.../AppConfig.kt` pon `API_BASE` con la URL de la carpeta (con `/` final).

## Publicar en GitHub (tú, desde tu Mac)

En [GitHub](https://github.com/new) crea un repositorio **vacío** (sin README) con el nombre que quieras, por ejemplo `callonlines-native-panel`.

En la terminal:

```bash
cd ~/Desktop/callonlines-native-panel
git remote add origin https://github.com/TU_USUARIO/callonlines-native-panel.git
git branch -M main
git push -u origin main
```

Si usas SSH:

```bash
git remote add origin git@github.com:TU_USUARIO/callonlines-native-panel.git
git push -u origin main
```

Opcional: instala [GitHub CLI](https://cli.github.com/) (`brew install gh`), ejecuta `gh auth login` y luego:

```bash
cd ~/Desktop/callonlines-native-panel
gh repo create callonlines-native-panel --public --source=. --remote=origin --push
```

## Seguridad

- Rota cualquier clave Magnus que haya aparecido en chats o código antiguo.
- `config.php` está en `.gitignore`; en CI/CD usa variables de entorno o secretos del repo.

## Licencia

Uso interno CallOnLines — ajusta la licencia si publicas el código.
