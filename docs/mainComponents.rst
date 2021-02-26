Componenti principali di ePAS
=============================

Il sistema ePAS è di tipo modulare (Figure 2). Il sistema si suddivide in tre
componenti: 

 * ePAS Server
 * ePAS Rest
 * ePAS Stamping Client. 
 
ePAS colleziona in un database locale dedicato tutte le informazioni relative 
ai dati gestiti dal sistema.

Il modulo **ePAS Server**, realizzato in Java tramite il web application 
framework Play Framework!, utilizzando le moderne tecnologie del Web (HTML5, 
CSS3, Bootstrap3, Javascript...) rende l'applicazione disponibile per la 
consultazione da parte degli utenti tramite l'utilizzo dei principali browser 
Internet sia da postazione fissa che da device mobile. 
Gli utenti del sistema possono consultare tramite web la propria situazione 
delle timbrature e delle assenze ed i resoconti delle loro informazioni legate 
all'orario. Tramite questo modulo l'ufficio del personale può aggiornare e 
tenere sotto controllo i dati di propria competenza.

Il modulo **ePAS Rest**, che utilizza ninjaframework, mette a disposizione le 
principali funzionalità di ePAS tramite un'interfaccia RESTful ed è fondamentale
per garantire l'integrazione di ePAS con gli eventuali altri sistemi presenti 
localmente agli istituti.
In particolare è tramite questo modulo che vengono inserite nel sistema le 
timbrature prelevate da eventuali lettori badge o da altri sistemi di 
rilevazione delle presenze.

Il modulo **ePAS Stamping Client**, sono una serie di script in parte in 
Python, in parte in Java che si occupano di interagire con i sistemi di 
rilevazione delle presenze, tipicamente con i lettori badge, estrarre dai 
lettori le informazioni ed inviarle via Rest al modulo ePAS Rest per il loro
salvataggio e trattamento.

.. figure:: _static/images/epas.png
   :scale: 35
   :align: center
   
   ePAS Architecture Overview