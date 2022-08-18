# ePAS - Electronic Personnel Attendance System

[![license](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?logo=gnu&style=for-the-badge)](https://github.com/consiglionazionaledellericerche/epas/blob/master/LICENSE)
[![Supported JVM Versions](https://img.shields.io/badge/JVM-11-brightgreen.svg?style=for-the-badge&logo=Java)](https://openjdk.java.net/install/)

ePAS è il nuovo sistema di rilevazione e gestione delle presenze del personale 
[CNR](https://www.cnr.it) sviluppato dall'Istituto [IIT](https://www.iit.cnr.it) in collaborazione
con l'Ufficio ICT; nasce nel 2012 come re-ingegnerizzazione di un sistema di rilevazione delle
presenze già sviluppato anni prima.

La documentazione completa del progetto è disponibile all'indirizzo

- [https://consiglionazionaledellericerche.github.io/epas/](https://consiglionazionaledellericerche.github.io/epas/)

ePAS consente l’integrazione con vari modelli di lettore badge per l'acquisizione delle timbrature
del personale ed è integrabile con varie componenti del sistema informativo di un Ente di Ricerca e
con sistemi di workflow paperless.

È stato realizzato come applicazione web, al fine di una sua immediata fruibilità da qualsiasi tipo
di sistema (PC, Tablet, Smartphone).

ePAS è attualmente utilizzato da più Enti di Ricerca.

[![license](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?logo=gnu&style=for-the-badge)](https://github.com/consiglionazionaledellericerche/epas/blob/master/LICENSE)
[![Version](https://img.shields.io/docker/v/consiglionazionalericerche/epas)](https://github.com/consiglionazionaledellericerche/epas/blob/main/VERSION)
[![Supported JVM Versions](https://img.shields.io/badge/JVM-11-brightgreen.svg?style=for-the-badge&logo=Java)](https://openjdk.java.net/install/)
[![contributors](https://img.shields.io/github/contributors/consiglionazionaledellericerche/cool-jconon.svg?logo=github&style=for-the-badge)](https://github.com/consiglionazionaledellericerche/epas/contributors/)
[![Docker Pulls](https://img.shields.io/docker/pulls/consiglionazionalericerche/epas.svg?logo=docker&style=for-the-badge)](https://hub.docker.com/r/consiglionazionalericerche/epas/)
[![ePAS on developers.italia.it](https://img.shields.io/badge/Italia-blue.svg?label=Selezioni&nbsp;online&style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAABd1BMVEUAZM0AYswAYcwAYs0Ea88Fa88Das8GbNACaM4Gac/B3/Xg8Pra7fnn9fx8suYAXMoHaM/u9fz///+YwOsAWcoFaM7m8fqTvuoAXMsAWsoAW8oAX8sAX8yOu+kAU8gAXssAYMwAY81Cj9t5seZUmt4EaM4LbNAhetQcd9Mdd9MgedShx+0VctIjetQle9V3sea01PFoqeP8/v/9//88jdoAXcv9/v/e7PkJa8/u9vw/jNprq+Tj7/oCZs4ed9NlouFpquM7jNrl8Prs9fxCjttpquT6/f7v9/3y+f36/v/S5vcFZ85anN/1+v2fx+2QvuoAZs4Nb9EOcNEOb9ERcNE8jNolfdWHt+gAVciKuegBZs7g7fmSv+rR5ffl8vs5i9kScdEfeNQZdNK00/H3+/7D2/RopuLR5vdwruVAj9sMbtDn8/vl8fsAZc1HlN251/Ku0PCv0PAogNYoftWexu3h7vn2/P7t9fzW5/eXwusBZc0PcNESc9L054SXAAABXklEQVQ4y72TZ1/CMBDG29SFqxpBu6SJigMXKlUcuLfinrj33nt8eC/Q8qssX8m9epL8c8lzl3DcvwSPEBLSrAtZ2Tm5eXzq/Y78gsKiYjElgEpKMcZlztSAqxyAihjAS7KCIkJVNU2wgEq3rhPKeFpVXeMhsF5bV9/g9Qgm0NjU3OKlQCitMPKBK6UNt3dgv2ECuBPjLhdYCXSzUY/Byb1M+EULgOgLAtAfkQMy5xxkYkiyAcNBuJJjhMlRiZPHfgHjE5NT0zOQQZoNYTxHeQuwjphfWFxaZvWmxsrqmgo+4zKsbxCCooUghs78xgGbCZWMOyIRMF2If2WQMgHw4S0Q225VI8kBinZA7O7tHxyipAAnHkU6cYxPNGq3GQPkU7NZZ8QOyOds7gIAcnkVBa5v7E/buL27f/A9qkw+Pb+EXt/ePz7tGSiRFMWIXktXwl/fAWTwaT4X9JJymYkfJHxA0uanFlQAAAAASUVORK5CYII=)](https://developers.italia.it/it/software/cnr-consiglionazionaledellericerche-epas)
[![Build doc Status](https://github.com/consiglionazionaledellericerche/epas/actions/workflows/pages/pages-build-deployment/badge.svg)](https://github.com/consiglionazionaledellericerche/epas/actions/workflows/pages/pages-build-deployment)

## Funzionalità per il dipendente

ePAS offre al dipendente le seguenti principali funzionalità:

-  consultazione della propria situazione presenze giornaliera/mensile/annuale;
-  timbrature, assenze, missioni, riepilogo orari di lavoro;
-  riepilogo ferie/riposi compensativi utilizzate e residue;
-  competenze mensili e annuali.

## Funzionalità per gli amministatori del personale

Dispone, inoltre, di un sistema di gestione che consente, agli uffici
del personale di:

-  inserire, modificare e cancellare il personale afferente
   all’istituto/UO;
-  gestire le varie tipologie di orario consentite dal Regolamento CNR;
-  inviare, a fine mese, gli attestati di presenza del personale
   (sistema integrato con la procedura “Attestati” del CNR);
-  disporre, in generale, della completa amministrazione e gestione
   delle informazioni;
-  pianificare, gestire e validare i calendari di servizi di turno e
   reperibilità.

## ePAS per il CNR

Per il CNR il servizio è installato presso la sede centrale del CNR ed è attualmente integrato con:

  - [Siper](https://consiglionazionaledellericerche.github.io/docs/siper) 
    (per poter ricavare i dati del personale);
  - il nuovo sistema 
    [Attestati](https://consiglionazionaledellericerche.github.io/docs/attestati.html)
    (per l'invio mensile degli attestati di  presenza);
  - [OIL](https://consiglionazionaledellericerche.github.io/docs/attestati.html) 
    (per la gestione delle segnalazioni e delle richieste di assistenza);
  - Identity Provider del CNR (per l'autenticazione tramite le credenziali Siper);
  - [Missioni](https://consiglionazionaledellericerche.github.io/docs/missioni.html) 
    (per l'inserimento automatizzato dei codici di missione).

## Applicazioni on line

* [ePAS - CNR](https://epas.amministrazione.cnr.it)
* [ePAS - INAF](https://epas.inaf.it)
* [ePAS - Consorzio Lamma](https://epas.lamma.toscana.it)

## 👏 Come Contribuire 

Lo scopo principale di questo repository è continuare ad evolvere ePAS. 
Vogliamo contribuire a questo progetto nel modo più semplice e trasparente possibile e siamo grati
alla comunità per ogni contribuito a correggere bug e miglioramenti.

## 📄 Licenza

ePAS è concesso in licenza GNU AFFERO GENERAL PUBLIC LICENSE, come si trova nel file [LICENSE][l].

[l]: https://github.com/consiglionazionaledellericerche/epas/blob/master/LICENSE

# <img src="https://www.docker.com/wp-content/uploads/2021/10/Moby-logo-sm.png" width=80> Startup

#### _Per avviare una istanza di ePAS con postgres locale_

ePAS può essere facilmente installato via docker-compose su server Linux utilizzando il file 
docker-compose.yml presente in questo repository.

Accertati di aver installato docker e docker-compose dove vuoi installare ePAS ed in seguito
esegui il comando successivo per un setup di esempio.

```
curl -fsSL https://raw.githubusercontent.com/consiglionazionaledellericerche/epas/master/epas-first-setup.sh -o epas-first-setup.sh && sh epas-first-setup.sh
```

Collegarsi a http://localhost:9000/ username: _admin_ password _cambialaosarailicenziato_ (da cambiare il prima possibile). 

#### _Avviare ePAS in alta affidabilità in un cluster docker swarm_

Un esempio di installazione di ePAS tramite *Ansible* in un cluster *Docker Swarm*
è disponibile, grazie ad Andrea Dell'Amico (ISTI - CNR), all'indirizzo:
[https://gitea-s2i2s.isti.cnr.it/ISTI-ansible-roles/ansible-role-epas.git](https://gitea-s2i2s.isti.cnr.it/ISTI-ansible-roles/ansible-role-epas.git)

Attenzione: per usare l'ultima versione opensource è necessario cambiare la variabile
*epas_docker_server_image* in **consiglionazionalericerche/epas**.

Il role *Ansible* configura anche un HAProxy, l'autenticazione tramite LDAP (che potrebbe essere
opzionale ed è eventualmente da personalizzare) e l'SMTP (anche questo da personalizzare).

## Credits

[Istituto di Informatica e Telematica del CNR](https://www.iit.cnr.it)

  - Cristian Lucchesi <cristian.lucchesi@iit.cnr.it>
  - Maurizio Martinelli <maurizio.martinelli@iit.cnr.it>
  - Dario Tagliaferri <dario.tagliaferri@iit.cnr.it>

## Vedi anche

  - [Documentazione completa di ePAS ](https://consiglionazionaledellericerche.github.io/epas/)
  - [ePAS client - file locali / ftp /sftp e lettori smartclock](https://github.com/consiglionazionaledellericerche/epas-client)
  - [ePAS client - timbratura da database SQL](https://github.com/consiglionazionaledellericerche/epas-client-sql)
