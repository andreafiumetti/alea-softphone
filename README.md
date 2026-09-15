# ALEA Softphone (Android)

App softphone Android per **ALEA PBX**, basata su [linphone-android](https://github.com/BelledonneCommunications/linphone-android) 6.2.7 (`1fde063`).

Il centralino e l’admin UI stanno nel repo separato: [andreafiumetti/alea-pbx](https://github.com/andreafiumetti/alea-pbx).

**Rilascio:** a ogni release prodotto aggiornare questo repo **e** `alea-pbx` (vedi `docs/PIANO-LAVORO.md` nel repo PBX).

## Cosa include

- Branding ALEA (icone, splash, temi)
- Enroll / QR join verso l’API ALEA PBX
- Sync rubrica aziendale (directory)
- Customizzazioni UI (drawer, liste, transfer)

## Build

Requisiti: Android Studio / JDK 17, SDK Android.

```bash
./gradlew assembleDebug
# oppure
./gradlew assembleRelease
```

APK di debug tipico: `app/build/outputs/apk/debug/`.

Note di build e gap API: vedi `docs/`.

## Upstream Linphone

Questo repo **non** tiene `origin` puntato a Belledonne: è uno snapshot lavorabile con le patch ALEA.

Per ri-allineare a un nuovo release Linphone:

1. Clona/aggiorna linphone-android alla tag desiderata
2. Ri-applica (o cherry-pick) i file sotto `app/src/main/java/org/linphone/alea/` e le modifiche brand/UI

## Licenza

Il codice Linphone resta sotto la licenza del progetto upstream (GPL). Le parti ALEA (`org.linphone.alea`, asset brand) sono proprietà ALEA / uso interno prodotto.
