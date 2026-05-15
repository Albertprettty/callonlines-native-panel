# CallOnLines — Panel cliente nativo + API JSON

## Estado del repo (local)

Este proyecto ya está en tu Mac con **Git inicializado** y el **primer commit** hecho:

**Ruta:** `~/Desktop/callonlines-native-panel`

Desde aquí solo falta **crear el repositorio vacío en GitHub** y hacer `git push` (yo no puedo entrar a tu cuenta GitHub desde aquí). Los comandos están más abajo en la sección **Publicar en GitHub**.

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
cd ~/Desktop/CallOnLines-Native-API
git init
git add .
git commit -m "Initial commit: API JSON + Android panel cliente"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/callonlines-native-panel.git
git push -u origin main
```

Si usas SSH:

```bash
git remote add origin git@github.com:TU_USUARIO/callonlines-native-panel.git
git push -u origin main
```

Opcional: instala [GitHub CLI](https://cli.github.com/) (`brew install gh`), ejecuta `gh auth login` y luego:

```bash
cd ~/Desktop/CallOnLines-Native-API
gh repo create callonlines-native-panel --public --source=. --remote=origin --push
```

## Seguridad

- Rota cualquier clave Magnus que haya aparecido en chats o código antiguo.
- `config.php` está en `.gitignore`; en CI/CD usa variables de entorno o secretos del repo.

## Licencia

Uso interno CallOnLines — ajusta la licencia si publicas el código.
