# Gap distinti — licensing, push, API (il fork non li chiude)

Documento separato dalla prova tecnica audio. PBX invariato in questa fase.

## 1. Licensing (decisione titolare)

| Componente | Dichiarazione upstream (da verificare sul tag usato) |
|---|---|
| App linphone-android 6.2.7 | Dual: **GPLv3** oppure licenza proprietaria Belledonne a pagamento |
| linphone-sdk-android 5.5.19 | Verificare README/LICENSE SDK e dipendenze native (codec, ecc.) nella revisione usata |
| Dipendenze app (Firebase, Coil, AppAuth, …) | Audit per distribuzione store / rivendita |

**Decisione 2026-11:** percorso **GPLv3** (vedi `softphone-v2/BELLEDONNE-COMPLIANCE.md` e `NOTICE`).  
Licenza proprietaria Belledonne **non** richiesta. Store ok solo con attribution + sorgenti disponibili agli utenti.

## 2. Push / raggiungibilità background

Il fork **non** eredita push funzionanti per ALEA:

- `google-services.json` ufficiale Linphone è per *loro* app / servizi; non riusabile come prodotto ALEA.
- Servono package name e credenziali FCM/APNs **proprie**.
- Con PBX invariato e limiti attuali di wake-up, **non** promettere piena raggiungibilità con app sospesa/killed su iOS/Android.
- Flexisip push gateway = opzione futura da valutare, **non** da installare in questa prova.

Traguardo B/C: infrastruttura push autorizzata; traguardo A: telefonia in foreground.

## 3. API / servizi prodotto ALEA mancanti

Chat UI Linphone ≠ chat ALEA. Stesso per:

| Requisito prodotto finale | Stato con solo fork |
|---|---|
| Multiaccount simultaneo | Da **verificare** sul tag 6.2.x (non assumere = completo) |
| Rubrica aziendale | Non creata dal fork; serve API/dati ALEA |
| Team / presenza | Idem |
| Voicemail visuale | MWI ≠ casella visuale; serve API |
| Chat (msg, gruppi, storico, allegati) | Serve API mobile autorizzata; no bypass admin API |
| Provisioning QR ALEA | Serve **adapter**; non compatibilità automatica col wizard Linphone |
| Server multi-installazione | Prodotto deve accettare host/account di installazioni diverse (non fissare solo VPS prova) |

## 4. iOS

Obbligatorio per prodotto finale. Prova build+chiamata da pianificare subito dopo (o in parallelo stretto) alla validazione Android audio, prima di investire UI Android ALEA.
