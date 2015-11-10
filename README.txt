ePAS - Electronic Personnel Attendance System
==============================================
  
Avvio da Eclipse
----------------

Per il run su eclipse utilizzare questo parametro vmargs:

 -javaagent:"${project_loc:Personnel Attendance System}/lib/lombok-1.16.4.jar"

Per java>=8 aggiungere -noverify per java<8 aggiungere -XX:-UseSplitVerifier

Test
----

Strumenti utilizzati:
 - jailer- http://jailer.sourceforge.net/ per l'estrazione dei dati dalla base di dati di produzione
   - la base di dati per i test è stata creata a partire dai dati delle "Person" admin e Cristian Lucchesi al 28 febbraio 2014
   
 - dbUnit - http://dbunit.sourceforge.net/
  - i dati sono importati e disponibili per i test tramite l'importazione del dataset db unit fatto dalla procedura Startup dentro il
    package dei test
    
 - junit
   - alcuni test di base sono fatti tramite l'integrazione in play della junit
 
QueryDSL:
Per ricompilare i Q<model>:
(meglio se si utilizza java6 per la seguente)

$ ant build -Dplay.path=<il-path-del-play>

Esempio di query sulle person:

        SearchResults<?> results = PersonDao.list(Optional.of(""), 
                ImmutableSet.of(Office.<Office>findById(1L)), true)
                .paginated(page);
        ...
