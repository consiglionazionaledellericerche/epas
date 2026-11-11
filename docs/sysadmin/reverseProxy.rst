Configurazione del reverse proxy
================================

In produzione ePAS deve essere servito tramite un reverse proxy (Apache, nginx, Traefik,
HAProxy...) che gestisce l'https e inoltra le richieste all'applicazione sulla porta 9000.
Il server HTTP integrato nell'applicazione non è pensato per essere esposto direttamente:
oltre a terminare il TLS, il proxy rifiuta le richieste HTTP malformate prima che arrivino
a ePAS.

Questa pagina descrive i requisiti comuni a tutti i proxy e riporta alcuni esempi di
configurazione.

Requisiti
---------

#. **La porta 9000 deve essere raggiungibile solo dai proxy autorizzati**, altrimenti
   questi possono essere aggirati. Nel *docker-compose.yml* di esempio, per un proxy
   installato sullo stesso server, la porta è pubblicata solo sull'interfaccia locale:

   .. code-block:: yaml

      ports:
        - 127.0.0.1:9000:9000

   Se il proxy è un container nella stessa rete docker (es. Traefik) la sezione *ports* si
   può togliere del tutto. Non usare ``9000:9000``: pubblica la porta su tutte le interfacce
   e docker scavalca le regole di *ufw* e *firewalld*.

   Con la porta pubblicata solo su ``127.0.0.1``, per collegarsi direttamente ad ePAS da
   un'altra macchina (es. durante la prima installazione) si può usare un tunnel ssh:
   ``ssh -L 9000:localhost:9000 utente@server`` e poi aprire http://localhost:9000/.

#. **Il proxy deve impostare l'header X-Forwarded-For** con l'indirizzo reale del client.
   Il proxy esposto ai client deve scartare l'eventuale valore inviato dal client; se ci
   sono più proxy, quelli interni devono accettare l'header solo da proxy a monte fidati.
   ePAS lo usa per i controlli sugli indirizzi ip, ad esempio la whitelist della
   timbratura web.

#. **X_FORWARDED_SUPPORT** deve contenere l'indirizzo ip da cui il proxy raggiunge ePAS.
   Le richieste con l'header X-Forwarded-For che arrivano da altri indirizzi vengono
   rifiutate con un errore. Il valore di default ``all`` accetta l'header da chiunque ed è
   accettabile solo se nessun client può raggiungere direttamente ePAS e il proxy riscrive
   sempre l'header; all'avvio ePAS lo segnala nel log con un WARN.

   L'indirizzo da indicare dipende da dove si trova il proxy:

   * **proxy installato sull'host** (Apache o nginx davanti al container): le richieste
     arrivano al container dal *gateway della rete docker*, non da ``127.0.0.1``. Il
     gateway si legge con:

     .. code-block:: bash

        docker network inspect epas_default -f '{{(index .IPAM.Config 0).Gateway}}'

     dove ``epas`` è il nome del progetto compose (di default il nome della directory).
     Il gateway può cambiare se la rete viene ricreata: per renderlo stabile si può fissare
     la subnet nel *docker-compose.yml*, scegliendone una che non si sovrapponga alle reti
     locali:

     .. code-block:: yaml

        networks:
          default:
            ipam:
              config:
                - subnet: 172.30.0.0/24

     e impostare ``X_FORWARDED_SUPPORT=172.30.0.1``. Su un'installazione esistente la rete
     va ricreata con ``docker compose down && docker compose up -d``.

   * **proxy in un container** nella stessa rete (es. Traefik): l'indirizzo del container
     del proxy, da fissare con ``ipv4_address`` (vedi l'esempio di Traefik).

   * **ePAS non in docker**: l'indirizzo del proxy, ad esempio ``127.0.0.1`` se è sullo
     stesso server.

   Con più proxy in catena vanno elencati, separati da virgola, gli indirizzi dei proxy
   presenti nella catena ``X-Forwarded-For`` e quello del proxy che raggiunge ePAS.

#. **PROTOCOL=https** se il proxy serve ePAS in https, così gli indirizzi generati
   dall'applicazione usano lo schema corretto.

Apache
------

Gli esempi sono per Ubuntu/Debian. Su RHEL e derivate i file di configurazione vanno in
``/etc/httpd/conf.d/`` e il servizio si chiama *httpd*.

Moduli necessari:

.. code-block:: bash

   sudo a2enmod proxy proxy_http headers ssl reqtimeout

Impostazioni globali
~~~~~~~~~~~~~~~~~~~~

Queste direttive vanno a livello di server e non nel VirtualHost: la request line e gli
header vengono letti prima che Apache scelga il virtual host, quindi dentro il VirtualHost
potrebbero non avere effetto. Creare il file
``/etc/apache2/conf-available/epas-hardening.conf``:

.. code-block:: apache

   # Rifiuta con 400 le richieste con request line o header non conformi
   # (è il default da Apache 2.4.25, meglio renderlo esplicito)
   HttpProtocolOptions Strict LenientMethods Require1.0

   # Limiti sugli header
   LimitRequestFields      50
   LimitRequestFieldSize   8190
   LimitRequestLine        8190

e abilitarlo con ``sudo a2enconf epas-hardening``.

Su Ubuntu/Debian il modulo *reqtimeout*, che protegge dai client che inviano le richieste
molto lentamente, è già configurato in ``/etc/apache2/mods-available/reqtimeout.conf``. Su
altre distribuzioni aggiungere al file:

.. code-block:: apache

   RequestReadTimeout header=20-40,MinRate=500 body=20,MinRate=500

Per non mostrare la versione di Apache e del sistema operativo, in
``/etc/apache2/conf-available/security.conf`` impostare:

.. code-block:: apache

   ServerTokens Prod
   ServerSignature Off

Una sola istanza di ePAS
~~~~~~~~~~~~~~~~~~~~~~~~

È la configurazione più comune: ePAS gira in docker sullo stesso server di Apache, con la
porta pubblicata su ``127.0.0.1:9000``. File ``/etc/apache2/sites-available/epas.conf``:

.. code-block:: apache

   <VirtualHost *:80>
       ServerName epas.example.it
       Redirect permanent / https://epas.example.it/
   </VirtualHost>

   <VirtualHost *:443>
       ServerName epas.example.it

       SSLEngine on
       SSLCertificateFile    /etc/letsencrypt/live/epas.example.it/fullchain.pem
       SSLCertificateKeyFile /etc/letsencrypt/live/epas.example.it/privkey.pem

       ErrorLog  ${APACHE_LOG_DIR}/epas_error.log
       CustomLog ${APACHE_LOG_DIR}/epas_access.log combined

       ProxyRequests     Off
       ProxyPreserveHost On

       # Scarta gli header inviati dal client: mod_proxy aggiunge X-Forwarded-For
       # e X-Forwarded-Host con i valori reali
       RequestHeader unset X-Forwarded-For  early
       RequestHeader unset X-Forwarded-Host early
       RequestHeader set   X-Forwarded-Proto "https"

       Header always unset X-Powered-By

       # disablereuse=On: una connessione verso ePAS per ogni richiesta, senza riuso
       ProxyPass        / http://127.0.0.1:9000/ disablereuse=On
       ProxyPassReverse / http://127.0.0.1:9000/
   </VirtualHost>

``disablereuse=On`` chiude la connessione verso ePAS dopo ogni richiesta. È una misura
prudenziale nell'esempio; comporta una nuova connessione per ogni richiesta e il suo
impatto va valutato in base al carico dell'installazione.

Questo VirtualHost assume che Apache riceva direttamente le richieste dei client. Se
c'è un altro proxy davanti, rimuovere sempre l'header ricevuto farebbe perdere l'IP
originale: accettare ``X-Forwarded-For`` solo dal proxy a monte fidato e adattare la
configurazione degli header alla catena effettiva.

Opzionalmente si può mostrare una pagina dedicata quando ePAS non è raggiungibile, ad
esempio durante un aggiornamento, copiando il file in ``/var/www`` e aggiungendo prima del
``ProxyPass /``:

.. code-block:: apache

   DocumentRoot /var/www
   ProxyPass /503-epas.html !
   ErrorDocument 503 /503-epas.html

Abilitare il sito e ricaricare Apache:

.. code-block:: bash

   sudo a2ensite epas
   sudo apache2ctl configtest && sudo systemctl reload apache2

Più istanze di ePAS (bilanciamento)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Per le installazioni in alta affidabilità (vedi :doc:`startup`) Apache può bilanciare le
richieste su più istanze con *mod_proxy_balancer*:

.. code-block:: bash

   sudo a2enmod proxy_balancer lbmethod_bybusyness

Rispetto all'esempio precedente cambia solo la parte del proxy: ``disablereuse=On`` va
indicato su ogni ``BalancerMember`` e ``ProxyPassReverse`` punta al balancer, così vale per
tutti i membri.

.. code-block:: apache

   <Proxy balancer://epas>
       BalancerMember http://127.0.0.1:9000 retry=5 max=100 disablereuse=On
       BalancerMember http://10.0.0.12:9000 retry=5 max=100 disablereuse=On
   </Proxy>

   <VirtualHost *:443>
       # ... ServerName, SSL, log e header come nell'esempio precedente ...

       ProxyPass        / balancer://epas/ lbmethod=bybusyness
       ProxyPassReverse / balancer://epas/
   </VirtualHost>

Per le istanze su altri server:

* la porta 9000 deve essere raggiungibile solo dal server con Apache, possibilmente su una
  rete privata. Con docker le regole di *ufw* non bastano: pubblicare la porta solo
  sull'interfaccia della rete privata (es. ``10.0.0.12:9000:9000``) oppure filtrarla nella
  catena ``DOCKER-USER`` di iptables;
* ``X_FORWARDED_SUPPORT`` deve contenere l'indirizzo con cui Apache si presenta a quel
  server.

Se si usa la pagina ``balancer-manager`` per abilitare o disabilitare i membri, va esclusa
dal proxy (prima del ``ProxyPass /``) e limitata agli indirizzi degli amministratori:

.. code-block:: apache

   ProxyPass /balancer-manager !
   <Location /balancer-manager>
       SetHandler balancer-manager
       Require ip 10.0.0.0/24
   </Location>

nginx
-----

nginx di default rifiuta gli header non validi (``ignore_invalid_headers on``) e verso il
backend usa HTTP/1.0 senza riutilizzare le connessioni: non è necessario cambiare questi
valori.

.. code-block:: nginx

   server {
       listen 80;
       server_name epas.example.it;
       return 301 https://$host$request_uri;
   }

   server {
       listen 443 ssl;
       server_name epas.example.it;

       ssl_certificate     /etc/letsencrypt/live/epas.example.it/fullchain.pem;
       ssl_certificate_key /etc/letsencrypt/live/epas.example.it/privkey.pem;

       server_tokens off;
       # dimensione massima delle richieste, da adeguare agli allegati caricati (default 1m)
       client_max_body_size 20m;

       location / {
           proxy_pass http://127.0.0.1:9000;
           proxy_set_header Host              $host;
           # $remote_addr sostituisce l'header del client invece di accodarsi.
           # Non usare $http_x_forwarded_for, che inoltra l'header del client
           proxy_set_header X-Forwarded-For   $remote_addr;
           proxy_set_header X-Forwarded-Host  $host;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }

Se davanti a nginx c'è un altro proxy, consentire l'accesso solo da quel proxy e
configurare ``set_real_ip_from`` con il suo indirizzo e ``real_ip_header X-Forwarded-For``.
Il proxy a monte deve scartare i valori ``X-Forwarded-For`` inviati dai client. Si può
quindi mantenere ``$remote_addr`` nell'esempio: nginx inoltra a ePAS l'indirizzo del
client riconosciuto tramite il proxy fidato, senza propagare valori non verificati.

Traefik
-------

Con Traefik 3.7 in un container nella stessa rete di ePAS la porta 9000 non va
pubblicata. Traefik scarta gli header X-Forwarded-* inviati dai client non fidati e li
riscrive.

Per avere un indirizzo stabile da indicare in ``X_FORWARDED_SUPPORT`` si fissa l'ip del
container di Traefik:

.. code-block:: yaml

   services:
     traefik:
       image: traefik:v3.7
       command:
         - --providers.docker=true
         - --providers.docker.exposedbydefault=false
         - --entrypoints.web.address=:80
         - --entrypoints.web.http.redirections.entrypoint.to=websecure
         - --entrypoints.websecure.address=:443
         # chiude la connessione verso il backend dopo ogni richiesta
         - --serverstransport.maxidleconnsperhost=-1
       ports:
         - 80:80
         - 443:443
       volumes:
         - /var/run/docker.sock:/var/run/docker.sock:ro
       networks:
         default:
           ipv4_address: 172.30.0.10

     epas:
       # ... come nel docker-compose.yml, senza la sezione ports ...
       labels:
         - traefik.enable=true
         - traefik.http.routers.epas.rule=Host(`${HOST_NAME}`)
         - traefik.http.routers.epas.entrypoints=websecure
         - traefik.http.routers.epas.tls=true
         - traefik.http.services.epas.loadbalancer.server.port=9000
       environment:
         - PROTOCOL=https
         - X_FORWARDED_SUPPORT=172.30.0.10

   networks:
     default:
       ipam:
         config:
           - subnet: 172.30.0.0/24

La configurazione dei certificati (es. Let's Encrypt) dipende dall'installazione e non è
riportata.

Verifica della configurazione
-----------------------------

Da una macchina non autorizzata a raggiungere direttamente ePAS, la porta 9000 non deve
rispondere (timeout o connessione rifiutata):

.. code-block:: bash

   curl -m5 -sI http://epas.example.it:9000/ ; echo "exit=$?"

Le richieste con header non validi devono essere rifiutate dal proxy con
``400 Bad Request``:

.. code-block:: bash

   printf 'GET / HTTP/1.1\r\nHost: epas.example.it\r\nFoo : bar\r\nConnection: close\r\n\r\n' \
     | openssl s_client -quiet -connect epas.example.it:443 -servername epas.example.it 2>/dev/null | head -1

Infine, nel log di ePAS all'avvio non deve comparire il WARN su ``XForwardedSupport=all``
e, se si usa la timbratura web con la whitelist degli indirizzi ip, le timbrature devono
essere accettate dalle postazioni autorizzate. Se compare l'errore
*This proxy request is not authorized* l'indirizzo indicato in ``X_FORWARDED_SUPPORT`` non
è quello da cui il proxy raggiunge ePAS: l'indirizzo corretto è riportato nel messaggio.
