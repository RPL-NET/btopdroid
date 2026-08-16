# btop-widget-android

Widget d'écran d'accueil Android natif, inspiré de btop — RAM, batterie, stockage, réseau, uptime, horloge, en style terminal (barres ASCII, fond noir, texte vert).

## Limites connues

- **CPU par cœur**: bloqué sans root depuis Android 8+ (SELinux restreint `/proc/stat`). Le widget tente une lecture globale approximative et affiche `n/a` si le device la bloque — comportement normal, pas un bug.
- **Refresh**: Android limite `updatePeriodMillis` à 30 min minimum pour les widgets. Un `WorkManager` périodique complète à 15 min (minimum permis). Taper sur le widget force un refresh immédiat.

## Build

```bash
./gradlew assembleDebug
```

APK généré dans `app/build/outputs/apk/debug/app-debug.apk`.

## Stack

Kotlin, `AppWidgetProvider` + `RemoteViews` (texte monospace, pas de Canvas custom — plus compatible entre versions Android), `WorkManager` pour le refresh périodique.
