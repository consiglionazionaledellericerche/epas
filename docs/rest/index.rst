Servizi REST di ePAS
====================

ePAS comprende una API Rest che permette l'integrazione con altri software e servizi che possono
aver bisogno delle informazioni contenute in ePAS o che necessitano di integrarsi con le 
unzionalità fornite dal sistema.
Le API rest sono soggetto ad autenticazione di tipo *Basic Auth* ed l'accesso è autorizzato
allo stesso meccanismo di controllo delle autorizzazioni presente in ePAS.
In particolare sono presenti due livelli di autenticazione per l'accesso alla API Rest:

* gli account per sede

  * possono accedere solo ai dati della sede che li ha creati
  * possono essere creati dagli *amministratori tecnici* delle singole sedi

* gli account di sistema

  * possono accedere trasversalmente ai dati di tutte le sedi
  * possono essere creati solo dagli admin dell'installazione di ePAS

Gli esempi sono per semplicità basati sulla `httpie <https://httpie.org/>`_ ed utilizzano la demo
disponibile all'indirizzo `https://epas-demo.devel.iit.cnr.it <https://epas-demo.devel.iit.cnr.it>`_ .

Naturalmente gli stessi comandi che trovate di seguito possono essere eseguiti anche nella istanza
di ePAS in produzione del vostro ente.

.. toctree::
   :maxdepth: 2
   :caption: Indice dei contenuti

   absencesServices
   certificationsServices
   contractsServices
   groupsAndAffiliationsServices
   leavesServices
   personDaysServices
   competencesServices
   personsServices
   childServices
   stampingsServices
   vacationServices
   workingTimeTypesServices
   missionsServices
   badgeServices
   absenceTypesServices
   mealTicketsServices

