# ALEA Softphone — listing store

Brand / sito: **https://www.aleaconnect.it/**  
Supporto: **support@aleaconnect.it**  
Privacy: https://www.aleaconnect.it/alea-pbx/privacy-softphone.php  
Termini: https://www.aleaconnect.it/alea-pbx/terms-softphone.php  
Sorgenti: https://github.com/andreafiumetti/alea-softphone · https://github.com/andreafiumetti/alea-softphone-ios  

Package Android: `it.aleaconnect.softphone`  
Bundle iOS: `it.aleaconnect.softphone` (unlisted)

## Titolo

ALEA Softphone

## Sottotitolo / short description (IT ≤80)

Client softphone per centralini ALEA PBX — chiamate, chat e casella.

## Descrizione lunga (IT)

ALEA Softphone è l’app ufficiale per i centralini **ALEA PBX** di ALEA Connect (www.aleaconnect.it).

Funzioni principali:
- Chiamate SIP (audio) verso interni e destinazioni del vostro centralino
- Chat di gruppo ALEA (API del PBX)
- Casella vocale e rubrica aziendale
- Enroll sicuro via QR dall’admin ALEA PBX

Basata su **Linphone** di Belledonne Communications e rilasciata sotto **GPLv3**.  
Non è l’app ufficiale Linphone e non usa il servizio Free SIP sip.linphone.org.

Privacy: https://www.aleaconnect.it/alea-pbx/privacy-softphone.php  
Termini: https://www.aleaconnect.it/alea-pbx/terms-softphone.php  
Codice sorgente: https://github.com/andreafiumetti/alea-softphone  

Contatto: support@aleaconnect.it · https://www.aleaconnect.it/

## Short description (EN ≤80)

Softphone for ALEA PBX — calls, group chat and voicemail.

## Full description (EN)

ALEA Softphone is the mobile client for **ALEA PBX** by ALEA Connect (www.aleaconnect.it).

Features: SIP calling, ALEA group chat, voicemail and company directory, QR enroll from the PBX admin.

Based on **Linphone** by Belledonne Communications, licensed under **GPLv3**.  
Not the official Linphone app; does not use sip.linphone.org Free SIP.

Privacy / terms / source: see links above.  
support@aleaconnect.it · https://www.aleaconnect.it/

## Attribution (obbligatoria in descrizione / About)

“Based on Linphone by Belledonne Communications (GPLv3).”

## Play Console — Data safety (sintesi)

| Dato | Raccolto | Condiviso | Scopo |
|------|----------|-----------|-------|
| Account / SIP enroll | Sì (on-device + vostro PBX) | Solo verso il PBX enrollato | Funzione app |
| Messaggi chat | Sì (via PBX) | PBX del cliente | Messaggistica |
| Audio chiamate | Tempo reale | PBX / destinazioni dialplan | Telefonia |
| Token push FCM/APNs | Se push abilitato | PBX + Google/Apple push | Notifiche |
| Log diagnostici | Solo se utente li invia | Destinatario scelto dall’utente | Supporto |

Niente vendita dati. Nessun advertising ID richiesto.

## Screenshot checklist

- Enroll QR
- Lista conversazioni + Gruppi ALEA
- Chiamata in corso
- Casella vocale
- Help → licenze / attribution Belledonne

## Pre-upload

1. AAB firmato release (`alea-release.keystore`)
2. `google-services.json` Firebase progetto **ALEA** + `ALEA_FCM_SERVER_KEY` su ogni PBX
3. Tag GitHub `vX.Y.Z-alea` = binario caricato
4. Privacy + termini live su www.aleaconnect.it
