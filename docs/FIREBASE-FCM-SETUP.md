# Firebase FCM — progetto ALEA (obbligatorio per Play)

Senza questo, Android non riceve push con app in background/killed.

## 1. Console Firebase

1. Apri https://console.firebase.google.com/ con account ALEA Connect  
2. Crea progetto **alea-softphone** (o nome simile)  
3. Aggiungi app Android:
   - Package: `it.aleaconnect.softphone`
   - Nickname: ALEA Softphone  
4. Scarica `google-services.json` → copialo in  
   `softphone-v2/android/app/google-services.json`  
   (**non** commitare; già in `.gitignore`)

## 2. SHA-1 release (Play)

Fingerprint già estratti dal keystore / AAB `6.3.10-alea`:

- SHA-1: `65:98:13:75:3F:E8:C3:E9:A6:8C:1D:9F:A6:1C:DC:2B:74:1E:6C:5B`
- SHA-256: `38:0B:8E:0B:77:C5:FB:83:A4:A7:45:45:62:85:74:29:39:10:3E:1B:28:DE:3A:23:2F:35:73:3E:82:34:96:72`

```bat
keytool -list -v -keystore softphone-v2\android\app\alea-release.keystore -alias alea
```

Incolla SHA-1 / SHA-256 in Firebase → Impostazioni progetto → App Android.

## 3. Server key sul PBX

Firebase → Impostazioni progetto → Cloud Messaging:

- Preferito legacy **Server key** → su ogni installazione:

```bash
# /etc/alea-pbx/env
ALEA_FCM_SERVER_KEY=AAAA....
```

Poi `systemctl restart alea-pbx-api`.

Verifica: dopo enroll Android, in DB `softphone_devices.push_token` non vuoto; journal senza `ALEA_FCM_SERVER_KEY non configurato`.

## 4. Rebuild

```bat
cd softphone-v2\android
gradlew.bat bundleRelease --no-daemon
```

Output AAB: `app/build/outputs/bundle/release/`

## Note

- Non riusare `google-services.json` Linphone ufficiale.  
- iOS usa APNs già configurato su VPS (`ALEA_APNS_*`), indipendente da FCM.
