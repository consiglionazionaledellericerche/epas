Configurazione autenticazione in ePAS
=====================================

ePAS supporta vari tipi di autenticazione che sono stati aggiunti ed aggiornati
nel tempo per far fronte ad esigenze di integrazione con sistemi informativi
di varie pubbliche amministrazioni.

I tipi di autenticazione attualmente supportati sono quattro:

 - *locale ad ePAS*: utente e password sono memorizzate direttamente in ePAS
 - *ldap*: le credenziali degli utenti sono all'interno di un LDAP server
 - *saml / shibboleth*: tramite Identity Provider di tipo SAML esterno ad ePAS
 - *oauth / keycloak*: tramite Identity Provider di tipo OAuth esterno ad ePAS

L'autenticazione predefinita è quella di tipo **locale ad ePAS**.

È possibile configurazione il tipo di autenticazione tramite i parametri del
docker-compose ed una documentazione di minima è già presente nel 
`docker-compose.yml <https://github.com/consiglionazionaledellericerche/epas/blob/main/docker-compose.yml>`_
di esempio fornito su github.

Autenticazione locale ad ePAS
-----------------------------

Questa è l'autenticazione predefinita, non è necessario attivarla e non è possibile disattivarla.
Ogni qual volta viene inserita una nuova persona in ePAS il sistema genera un utente associato
alla persona che viene inserita.
Il nome utente viene generato concatenando nome e cognome con in mezzo un "." e togliendo
tutti gli eventuali spazi secondo la regola:

 - username = *nome*.replace(" ", "").lower() + "." + *cognome*.replace(" ", "").lower()

Per esempio lo username di "Don Chisciotte" "Della Mancia" sarebbe *donchisciotte.dellamancia*.

In casi di omonimia lo username viene mantenuto univoco inserendo un numero incrementale alla fine
dello username generato secondo la regola precedente.

Al momento della creazione dell'utente viene anche generata una password causale che non è mostrata
in nessuna interfaccia di ePAS.
Per impostarsi la password per accedere con le credenziali di ePAS gli utenti devono utilizzare
il link *Password dimenticata?* presente nella pagina di login con le credenziali locali ad ePAS.

La procedura di *Password dimenticata?* chiede all'utente la propria email e nel caso questa sia
presente nel sistema allora ePAS invia a quella email un link con le informazioni per impostarsi
la password.

Affinché la procedura di *Password dimenticata?* possa essere utilizzare è necessario aver
configurato correttamente l'SMTP server che ePAS utilizzerà per invia le email.
La configurazione del STMP server è documentata sempre nel docker-compose.yml di esempio citato sopra.



