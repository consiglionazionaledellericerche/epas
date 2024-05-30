Consultazione Uffici via REST
=======================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione della lista delle sedi 
preesnti nel sistema.

Gli esempi sono per semplicità basati sulla `httpie <https://httpie.org/>`_ ed utilizzano la demo
disponibile all'indirizzo `https://epas-demo.devel.iit.cnr.it <https://epas-demo.devel.iit.cnr.it>`_ .

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia uno dei
seguenti ruoli di sistema *Consultatore riepiloghi orari e assenze*, *Developer*, *Amministratore* e dalla
versione 2.15.0 anche dei ruoli *Gestore assenze* e *Gestore anagrafica*.

Gli utenti di sistema non possono essere creati dalle singole sedi ma da un utente con ruolo 
 *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Office List
-----------

Per ottenere la lista degli uffici **attivi** è possibile effettuara una *HTTP GET* all'indirizzo 
**/rest/v3/offices/all**.

È possibile indicare una data di riferimento per indicare gli uffici *attivi* in quella data:

  - atDate (in formato aaaa-mm-gg).

.. code-block:: bash

  $ http -a registry_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/offices/all atDate==2024-01-01

.. code-block:: json

  [
    {
        "address": "Via Moruzzi 1, Pisa",
        "beginDate": "2012-07-30",
        "code": "044000",
        "codeId": "223400",
        "endDate": null,
        "headQuarter": false,
        "id": 1,
        "instituteId": 1,
        "joiningDate": null,
        "name": "IIT - Pisa",
        "perseoId": 180,
        "updatedAt": "2022-03-15T11:42:53.909061"
    }
  ]
