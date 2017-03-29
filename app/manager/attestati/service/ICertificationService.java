package manager.attestati.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import manager.attestati.dto.show.CodiceAssenza;

import models.Certification;
import models.Office;
import models.Person;


public interface ICertificationService {

  /**
   * Se il token è abilitato alla sede.
   *
   * @param office sede
   * @param result result (da rimuovere)
   * @return esito
   */
  boolean authentication(Office office, boolean result);

  /**
   * Costruisce la situazione attestati di una persona.
   *
   * @param person  persona
   * @param year    anno
   * @param month   mese
   * @return lo stato
   */
  PersonCertData buildPersonStaticStatus(Person person, int year, int month)
      throws ExecutionException;

  /**
   * Se le due mappe contententi certificazioni sono equivalenti e non contengono errori.
   *
   * @param map1 map1
   * @param map2 map2
   * @return esito
   */
  boolean certificationsEquivalent(Map<String, Certification> map1,
      Map<String, Certification> map2);

  /**
   * Elaborazione persona.
   *
   * @param personCertData il suo stato
   * @return lo stato dopo l'elaborazione.
   */
  // TODO Questa parte andrebbe resa più semplice perchè per trasmettere le informazioni
  // ad attestati sono costretto ad avere un PersonCertData che è il risultato
  // ottenuto dal metodo buildPersonStaticStatus il quale a sua volta effettua una richiesta
  // ad attestati per il recupero delle informazioni della persona
  PersonCertData process(PersonCertData personCertData)
      throws ExecutionException, NoSuchFieldException;

  /**
   * Invia la certificazione ad attestati.
   */
  Certification sendCertification(Certification certification);

  /**
   * Rimuove il record in attestati. (Non usare per buoni pasto).
   */
  boolean removeAttestati(Certification certification)
      throws ExecutionException, NoSuchFieldException;

  /**
   * La lista dei codici assenza... TODO: conversione al tipo epas??
   *
   * @return lista
   */
  Map<String, CodiceAssenza> absenceCodes() throws ExecutionException;

}
