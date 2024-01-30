Calendari iCal
==============

Sono disponibili alcuni calendari in formato ics, per la reperibilità, per i turni e per le assenze.
Ogni persona può accedere ai proprio calendari, utilizzando l'autenticazione Basic Auth e le credenziali
locali di ePAS.

I calendari reperibilità e turni sono disponibili sono ai reperibili e turnisti, si può ottenere il 
calendario indicando il tipo del calendario (attributo *type*) e l'anno (attributo *year*).

Nel rispettive URL sono nei formati seguenti:
 * https://${epas-host}/reperibility/ical?type=${type}&year=${year}
 * https://${epas-host}/shift/ical?type=${type}&year=${year}

Per il calendario dei turni i parametri *type* e *year* sono opzionali, se il *type* non è specificato
vengono restituiti tutti i calendari di turno del turnista, mentre se l'*year* non è indicato viene 
restituito l'anno corrente.

Il calendario delle assenze è disponibile per tutti i dipendenti, è obbligatorio specificare una data 
di inizio e di fine, mentre opzionalmente si può specificare il codice dell'assenza che eventualmente
vogliamo mostrare nel calendario.
Il periodo massimo per cui si può richiedere il calendario delle assenze è di 1 anno.

La URL corrisponde al seguente formato:
 * https://${epas-host}/absences/ical?begin=${begin}&end=${end}&absenceCode=${absenceCode}
