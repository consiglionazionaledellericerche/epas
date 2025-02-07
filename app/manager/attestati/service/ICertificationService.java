/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.attestati.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import manager.attestati.dto.internal.CruscottoDipendente;
import manager.attestati.dto.internal.clean.ContrattoAttestati;
import manager.attestati.dto.show.CodiceAssenza;
import models.Certification;
import models.Office;
import models.Person;

/**
 * Funzionalità integrazione ePAS - Nuovo Attestati.
 *
 * @author Alessandro Martelli
 */
public interface ICertificationService {

  /**
   * Invalida la cache dell'oauth token.
   */
  void invalidateOauthTokenCache();

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
  
  /**
   * I dati contrattuali in attestati.
   *
   * @param office sede
   * @param year anno 
   * @param month mese
   * @return mappa matricola - contratto
   */
  Map<String, ContrattoAttestati> getCertificationContracts(Office office, int year, int month)
      throws ExecutionException, NoSuchFieldException;
  
  /**
   * Il periodo dipendente, solo per fare le prove. Questo metodo dovrà progressivamente 
   * diventare il metodo che scarica le assenze degli ultimi due anni di una persona.
   */
  CruscottoDipendente getCruscottoDipendente(Person person, int year) 
      throws ExecutionException, NoSuchFieldException;

}
