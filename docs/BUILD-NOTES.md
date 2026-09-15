# Build notes — linphone-android 6.2.7 (prova A)

## Esito

| Campo | Valore |
|---|---|
| Esito | **BUILD SUCCESSFUL** (assembleDebug) |
| App versionName | 6.2.7 |
| Git commit | `1fde063979b68608e10a043b981e5ddd829a3a9e` |
| Linphone SDK (Maven) | **5.5.19** (`org.linphone:linphone-sdk-android`) |
| APK | `app/build/outputs/apk/debug/linphone-android-debug-6.2.7.apk` |
| Copia stabile | `softphone-v2/android/dist/linphone-android-6.2.7-debug.apk` |
| Size | 133234595 byte |
| LastWriteTime | 09/09/2026 10:05:43 |
| **SHA256** | `0D915DD497FC3F68087E96E67B53502E30918308FD83FF407CEC1BD302F4E30B` |
| Firma | debug (lab) |
| Package | `org.linphone` |

## Ambiente

```
JAVA_HOME=C:\alea-pbx\.build-tools\jdk-21   (Temurin 21.0.12.1)
ANDROID_HOME=C:\alea-pbx\.build-tools\android-sdk
platforms: android-36.1, android-37.0 installati per soddisfare AGP/AAR
```

```bat
cd softphone-v2\android
gradlew.bat assembleDebug --no-daemon
```

Log: `build-assembleDebug.log`

## Diff rispetto a upstream puro

1. `local.properties` — sdk.dir locale (non in git tipicamente)  
2. `gradle/libs.versions.toml` — `linphone` **5.5.+ → 5.5.19** (riproducibilità)  
3. Nessuna modifica audio/lifecycle/UI prodotto  
4. compileSdk/targetSdk lasciati a **37** (come upstream) dopo install platform 37.0  

## Download lab (stesso Wi‑Fi PC)

http://192.168.2.93:8765/linphone-android-6.2.7-debug.apk  

**Disinstallare** l’APK Flutter ALEA prima delle prove (un client alla volta).

## Prove umane — NON ESEGUITE da Cursor

Checklist in `README.md`. Se fallisce audio: misure prima di patch.
