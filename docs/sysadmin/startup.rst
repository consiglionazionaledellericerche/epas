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

