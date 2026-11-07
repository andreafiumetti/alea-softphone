# Belledonne / Linphone — percorso GPLv3 ALEA

Decisione prodotto: **GPLv3 open** (niente licenza proprietaria Belledonne a pagamento).  
Scopo: distribuire **ALEA Softphone** (fork Linphone) su Play Store e App Store (iOS unlisted) in modo conforme.

Sito / brand: **https://www.aleaconnect.it/** · supporto: **support@aleaconnect.it**

> Non è parere legale formale. Prima del go-live store, far revisionare da consulente IP/software se serve.

## 1. Cosa impone Belledonne / GPLv3 (sintesi operativa)

| Tema | Obbligo | Stato ALEA |
|------|---------|------------|
| Licenza del fork | Distribuire sotto **GPLv3** | OK — `LICENSE.txt` |
| Attribution | Citare **Belledonne Communications** e **Linphone** | OK — `NOTICE`, Help → licenze |
| Sorgenti | Sorgente della build distribuita | OK — repo **pubbliche** + tag release |
| Conservare copyright | Non rimuovere header Belledonne | OK |
| Marchio | Non presentarsi come Linphone ufficiale | OK — branding ALEA |
| Servizi cloud Linphone | Non usare Free SIP `sip.linphone.org` | OK — default rimosso |
| Codec / SDK | Audit dipendenze native | Da rifare a ogni bump SDK |

## 2. Repo e offerta sorgente

| Repo | URL |
|------|-----|
| Android | https://github.com/andreafiumetti/alea-softphone |
| iOS | https://github.com/andreafiumetti/alea-softphone-ios |

In-app: Help → sorgenti / privacy. Offerta scritta: `support@aleaconnect.it` (`NOTICE`).

**A ogni rilascio store:** taggare la revisione esatta del binario; non commitare keystore / `google-services.json`.

## 3. URL prodotto (www.aleaconnect.it)

| Risorsa | URL |
|---------|-----|
| Sito | https://www.aleaconnect.it/ |
| Download PBX | https://www.aleaconnect.it/alea-pbx/ |
| Privacy softphone | https://www.aleaconnect.it/alea-pbx/privacy-softphone.php |
| Termini softphone | https://www.aleaconnect.it/alea-pbx/terms-softphone.php |

## 4. Checklist pre-store

### A. Legale / open source

- [x] Scrub `google-services.json` Linphone dalle repo pubbliche
- [x] Repo pubbliche Android + iOS
- [x] NOTICE + Help attribution
- [x] Privacy + termini su www.aleaconnect.it
- [x] Default `sip.linphone.org` rimosso
- [x] Tag GitHub = binario store corrente (`v6.3.10-alea`)
- [ ] Audit LICENSE SDK linphone-sdk revisione usata

### B. Marchio

- [x] Titolo app ALEA Softphone (non “Linphone”)
- [ ] Screenshot store senza logo Linphone
- [x] Descrizione con “Based on Linphone (Belledonne)” — vedi `STORE-LISTING.md`

### C. Apple (unlisted)

- [ ] App ID + Distribution
- [ ] App Store Connect unlisted
- [x] APNs su VPS (`ALEA_APNS_*`) — verificare su Connect
- [ ] Privacy nutrition labels

### D. Google Play

- [ ] Play Console listing + Data safety (bozza in `STORE-LISTING.md`)
- [x] Keystore release ALEA generato (`alea-release.keystore`, vault personale)
- [ ] `google-services.json` Firebase **ALEA** + `ALEA_FCM_SERVER_KEY` su PBX — vedi `docs/FIREBASE-FCM-SETUP.md`
- [x] AAB `bundleRelease` firmato (locale `softphone-v2/store-artifacts/6.3.10-alea/`) — upload Play Console ancora da fare

## 5. Cosa NON fare

- Distribuire binari store senza tag sorgente corrispondente
- Rimettere default `sip.linphone.org`
- Usare `google-services.json` / push Linphone ufficiale
- Commitare password keystore o Firebase

## 6. Ordine operativo rimanente

1. Firebase FCM ALEA → `google-services.json` + `ALEA_FCM_SERVER_KEY` su PBX (SHA-1 release sotto)  
2. Play Console → internal testing → carica AAB `alea-softphone-6.3.10-alea.aab`  
3. TestFlight / App Store unlisted iOS  
4. Collaudo kill: chiamata + chat push  

### Fingerprint keystore release (Firebase / Play App signing)

- SHA-1: `65:98:13:75:3F:E8:C3:E9:A6:8C:1D:9F:A6:1C:DC:2B:74:1E:6C:5B`
- SHA-256: `38:0B:8E:0B:77:C5:FB:83:A4:A7:45:45:62:85:74:29:39:10:3E:1B:28:DE:3A:23:2F:35:73:3E:82:34:96:72`  
