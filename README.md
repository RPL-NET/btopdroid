# btopdroid

Widget d'écran d'accueil Android natif, inspiré de [btop](https://github.com/aristocratos/btop) — RAM, batterie, stockage, réseau, uptime et horloge, rendu en style terminal (barres ASCII, fond noir, texte vert). Aucune app tierce requise, aucun accès root.

## Fonctionnalités

- RAM utilisée / totale, en barre + pourcentage
- Stockage utilisé / total
- Batterie (avec indicateur de charge)
- Débit réseau (down/up, delta calculé entre deux lectures)
- Uptime de l'appareil
- Horloge + date
- CPU global (best-effort — voir Limites)
- Tap sur le widget = refresh immédiat

## Limites connues

- **CPU par cœur**: bloqué sans root depuis Android 8+ (SELinux restreint l'accès à `/proc/stat` sur la plupart des devices). btopdroid tente une lecture globale approximative et affiche `n/a` si le système la bloque — c'est le comportement attendu, pas un bug.
- **Fréquence de refresh**: Android impose un minimum de 30 min pour `updatePeriodMillis` sur les widgets home screen. Un `WorkManager` périodique complète à 15 min (le minimum permis par l'OS). Taper sur le widget force un refresh immédiat sans attendre.

## Installation

Pas encore sur un store — build l'APK toi-même ou récupère-le dans les [Releases](../../releases) une fois publiées.

```bash
git clone https://github.com/RPL-NET/btopdroid.git
cd btopdroid
./gradlew assembleDebug
```

L'APK sort dans `app/build/outputs/apk/debug/app-debug.apk`. Installe-le (autorise "sources inconnues"), puis ajoute le widget depuis l'écran d'accueil: long-press → Widgets → btopdroid.

## Stack technique

Kotlin, `AppWidgetProvider` + `RemoteViews` (rendu texte monospace plutôt que Canvas custom, pour une meilleure compatibilité inter-versions Android), `WorkManager` pour le refresh périodique. Min SDK 26.

## Contribuer

Projet perso, ouvert aux PR si ça t'intéresse. Pas de roadmap formelle pour l'instant.

## License

MIT — voir [LICENSE](LICENSE).
