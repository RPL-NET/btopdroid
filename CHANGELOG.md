# Changelog

## 0.3.0
- APK de sortie renommé automatiquement (`btopdroid-<version>-<buildtype>.apk`) au lieu du générique `app-debug.apk`
- `versionCode`/`versionName` bump

## 0.2.0
- Ajout de `MainActivity` (page settings avec preview live) — certains launchers OEM (dont Motorola) n'affichent pas le widget d'une app dans le picker si l'app n'a aucune activité visible
- 5 presets de couleur (vert classique, ambre rétro, cyan, blanc sur noir, rouge alerte)
- 4 styles de barre ASCII (blocs pleins, ascii classique `#`/`-`, `=`, carrés)
- Fix: `android:exported="true"` sur le `BtopWidgetProvider` — sans ça le launcher (app séparée) ne peut pas énumérer le receiver pour le lister dans le sélecteur de widgets

## 0.1.0
- Premier build fonctionnel: widget natif `AppWidgetProvider` + `RemoteViews`
- Stats: RAM, stockage, batterie, réseau (delta), uptime, horloge/date
- CPU global en best-effort (`n/a` si bloqué par le système, normal sur Android 8+ sans root)
- Refresh: `updatePeriodMillis` (30 min, minimum OS) + `WorkManager` périodique (15 min) + tap pour refresh immédiat
