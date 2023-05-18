Startup dell'applicazione
=========================

Per avviare una istanza di ePAS con postgres locale
---------------------------------------------------

ePAS può essere facilmente installato via docker-compose su server Linux utilizzando il file 
docker-compose.yml presente in questo repository.

Accertati di aver installato docker e docker-compose dove vuoi installare ePAS ed in seguito
esegui il comando successivo per un setup di esempio.

.. code-block::

  curl -fsSL https://raw.githubusercontent.com/consiglionazionaledellericerche/epas/master/epas-first-setup.sh -o epas-first-setup.sh && sh epas-first-setup.sh

Collegarsi a http://localhost:9000/ username: *admin* password *cambialaosarailicenziato* (da cambiare il prima possibile). 

Avviare ePAS in alta affidabilità
------------------------------------------------------------

ePAS è un'applicazione in cui lo stato risiede principalmente nel database, in quanto 
la sessione http è mantenuta all'interno del client (tramite un cookie).
Questa caratteristica permette di avere più instanze di ePAS e di poterle bilanciare 
tramite un proxy http (tipo Traefik, HAProxy, Ngnix, Apache, etc), senza bisogno di usare 
meccanismo tipo sticky bit.

Ci sono però tre aspetti da tenere da considerazione:

#. le istanza di ePAS hanno all'interno dei job (tipo cron). È necessario che una sola 
   istanza abbia i job attivi, è possibile controllare l'attivazione dei job tramite
   la variabile d'ambiente JOBS_ACTIVE, solo una delle istanza deve avere JOBS_ACTIVE=true,
   nelle altre deve essere impostato JBOS_ACTIVE=false

#. le istanza di ePAS hanno all'interno un meccanismo per aggiornare le evoluzioni del database.
   È necessario che una sola istanza abbia le evoluzioni del database attivate, è possibile 
   controllare l'attivazione delle evoluzioni del database tramite la variabile d'ambiente 
   EVOLUTIONS_ENABLED, solo una delle istanza deve avere EVOLUTIONS_ENABLED=true, nelle altre 
   deve essere impostato EVOLUTIONS_ENABLED=false

#. ePAS permette di caricare degli allegati associandoli alle assenze e questi allegati vengono
   salvati nel file system del server ospitante l'instanza docker.
   La directory dove vengono salvati gli allegati si chiama attachments ed è montata solitamente
   come volume nel docker-compose.yml:

	.. code-block::

	   volumes:
	      - ./epas/attachments:/home/epas/epas/data/attachments

   Se usate questa funzionalità è necessario che la directory ./epas/attachments sia condivisa
   tra i vari server che ospitano le istanze docker, per esempio tramite l'utilizzo di NFS.


Avviare ePAS in alta affidabilità in un cluster docker swarm
------------------------------------------------------------

Un esempio di installazione di ePAS tramite *Ansible* in un cluster *Docker Swarm*
è disponibile, grazie ad Andrea Dell'Amico (ISTI - CNR), all'indirizzo:
`https://gitea-s2i2s.isti.cnr.it/ISTI-ansible-roles/ansible-role-epas.git <https://gitea-s2i2s.isti.cnr.it/ISTI-ansible-roles/ansible-role-epas.git>`_

Attenzione: per usare l'ultima versione opensource è necessario cambiare la variabile
*epas_docker_server_image* in **consiglionazionalericerche/epas**.

Il role *Ansible* configura anche un HAProxy, l'autenticazione tramite LDAP (che potrebbe essere
opzionale ed è eventualmente da personalizzare) e l'SMTP (anche questo da personalizzare).

Configurazione funzionalità di ePAS
-----------------------------------

Le funzionalità principali di ePAS possono essere configurate tramite le variabili
d'ambiente presenti nel `docker-compose.yml <https://github.com/consiglionazionaledellericerche/epas/blob/main/docker-compose.yml>`_ presente nel repo github.
La configurazione delle possibili modalità di autenticazione e la configurazione dei log
sono descritte nei paragrafi successivi.

Configurazione Application Secret
---------------------------------

**N.B.**
Si raccomanda di modificare la varibile d'ambiente **APPLICATION_SECRET** per impostare
una propria chiave univoca utilizzata nel funzioni di cifratura.

L'application secret può per esempio essere creato su Linux con questo comando:

.. code-block::

  tr -dc A-Za-z0-9 < /dev/urandom | head -c 64 ; echo

