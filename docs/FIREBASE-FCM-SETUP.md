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
