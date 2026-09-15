# Softphone v2 — prova isolata linphone-android

Data avvio: 9 settembre 2026.  
Riferimento: `VALUTAZIONE-BASE-OSS-CODEX.md`.  
**PBX invariato. `softphone/` Flutter invariato (nessuna copia helper).**

## Tre traguardi (distinti)

| Traguardo | Significato | Stato |
|---|---|---|
| **A — Prova tecnica** | Upstream linphone-android compilato, installato, audio bidirezionale + flussi telefonici sul telefono del caso fallito | **IN CORSO** (build) |
| **B — Migrazione** | Branding ALEA, enroll QR/adapter, default server configurabile (non hardcodare solo VPS), package name ALEA | **NON INIZIATA** — solo dopo A positiva |
| **C — Prodotto completo** | Android **e** iOS; multiaccount simultaneo; rubrica; team; voicemail visuale; chat ALEA; push propri; API mobile | **OBBLIGATORIO finale** — il fork **non** lo completa da solo (vedi `GAP-LICENSING-PUSH-API.md`) |

## Checkout

| Campo | Valore |
|---|---|
| Path | `softphone-v2/android/` |
| Upstream | https://github.com/BelledonneCommunications/linphone-android |
| Tag | **`6.2.7`** |
| Commit | `1fde063979b68608e10a043b981e5ddd829a3a9e` |
| SDK Maven | `org.linphone:linphone-sdk-android` **`5.5.19`** (pinnato; upstream aveva `5.5.+`) |
| Package | `org.linphone` (invariato per prova A) |

## Modifiche ambiente (non prodotto / non audio)

Solo per compilare sul lab Windows:

1. `local.properties` → `sdk.dir` locale  
2. `compileSdk` / `targetSdk` **37 → 36** (platform 37 assente nello SDK locale)  
3. Pin SDK `5.5.+` → `5.5.19` per riproducibilità  

**Nessuna** modifica a routing audio, lifecycle chiamata, UI/branding ALEA.

## Build

Vedi **`BUILD-NOTES.md`** (SUCCESS).

| Campo | Valore |
|---|---|
| SDK | 5.5.19 |
| SHA256 | `0D915DD497FC3F68087E96E67B53502E30918308FD83FF407CEC1BD302F4E30B` |
| Prove umane | **PENDING** |

```text
JAVA_HOME=C:\alea-pbx\.build-tools\jdk-21
ANDROID_HOME=C:\alea-pbx\.build-tools\android-sdk
cd softphone-v2/android
gradlew.bat assembleDebug --no-daemon
```

Esito / hash APK: in `BUILD-NOTES.md`.

## Protocollo prove (stesso telefono del fallimento Flutter)

Un client alla volta (disinstallare softphone ALEA Flutter prima):

1. app → interno (bidirezionale)  
2. interno → app  
3. esterno → app  
4. mute/unmute, speaker, cuffie  
5. hold/resume  
6. DTMF / IVR  
7. trasferimenti  
8. due account stessa VPS + due server (uscente corretto; rimozione isolata)  

Se audio fallisce: **misure prima**, niente patch cieche. Confrontare con diagnostica Flutter `1.2.3+10`.

## iOS

Pianificare **presto** build + chiamata su device (`softphone-v2/ios/` da linphone-iphone, tag da fissare).  
Senza macOS/iPhone: riportare **piattaforma non validata**, non “cross-platform ok”.

## Non fare

- Copiare `LinPhoneHelper` / plugin Flutter nella nuova base  
- Dichiarare prodotto completo dopo sola prova Android  
- Hardcodare `pbx.aleaconnect.it` nel codice prodotto (ok solo come server di prova manuale)
