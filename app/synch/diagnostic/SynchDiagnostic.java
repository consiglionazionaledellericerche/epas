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

package synch.diagnostic;

import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import models.Contract;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;

/**
 * Mette a disposizione servizi di diagnosi dello stato di sincronizzazione.
 * <p>
 * Istituto: Un istituto è correttamente sincronizzato se: 1) Ha il perseoId popolato e l'entità di
 * perseo associata è aggiornata. TODO: Cds, Sigla, Nome 2) Tutte le sue sedi di epas sono
 * correttamente sincronizzate
 *
 * Una sede è correttamente sincronizzata se: 1) Ha il perseoId popolato e l'entità di perseo
 * associata è aggiornata. 2) Tutte le persone con matricola in perseo sono caricate in epas e
 * sincronizzate.
 *
 * Una persona è correttamente sincronizzata se: 1) Se ha il perseoId popolato allora l'entità di
 * perseo associata è aggiornata 2) Tutti i suoi contratti con perseoId popolato sono correttamente
 * sincronizzati 3) Se su perseo è attiva: a) Ha il perseoId popolato b) Il contratto epas attivo è
 * correttamente sincronizzato
 *
 * Un contratto è correttamente sincronizzato se: 1) Ha il perseoId popolato e l'entità di perseo
 * associata è aggiornata. Data inizio, Data Fine (con gestione caso determinato)
 * </p>
 *
 * @author Alessandro Martelli
 */
public class SynchDiagnostic {

  /**
   * Comparazione seat epas <-> perseo.
   *
   * @return esito
   */
  private boolean seatEquals(Office epasOffice, Office perseoOffice) {

    if (!epasOffice.perseoId.equals(perseoOffice.perseoId)
        || !epasOffice.codeId.equals(perseoOffice.codeId)
        || !epasOffice.code.equals(perseoOffice.code)
        || !epasOffice.name.equals(perseoOffice.name)
        || !epasOffice.address.equals(perseoOffice.address)) {
      return false;
    }

    //Stesso istituto ...
    return epasOffice.institute.perseoId != null
        && perseoOffice.institute.perseoId != null
        && Objects.equals(epasOffice.institute.perseoId, perseoOffice.institute.perseoId);
  }

  /**
   * Verifica se l'ufficio su ePAS e su Perseo contengono gli stessi dati.
   */
  public boolean isSeatSynchronized(Office epasOffice, Office perseoOffice,
      Map<String, Person> epasPeople, Map<String, Person> perseoPeople,
      Map<String, List<Contract>> perseoContracts) {

    if (epasOffice.perseoId == null) {
      return false;
    }
    if (!seatEquals(epasOffice, perseoOffice)) {
      return false;
    }

    //Tutte le persone con matricola in perseo sono caricate in epas e sincronizzate.
    for (Person perseoPerson : perseoPeople.values()) {
      Person epasPerson = epasPeople.get(perseoPerson.number);
      if (epasPerson == null) {
        return false;
      }

      if (!isPersonSynchronized(epasPerson, perseoPerson,
          perseoContracts.get(perseoPerson.number))) {
        return false;
      }
    }

    return false;
  }

  /**
   * Comparazione persone epas <-> perseo.
   */
  public boolean personEquals(Person epasPerson, Person perseoPerson) {

    if (epasPerson.qualification == null) {
      return false;
    }

    if (!epasPerson.name.equals(perseoPerson.name)
        || !epasPerson.surname.equals(perseoPerson.surname)
        || !epasPerson.number.equals(perseoPerson.number)
        || !epasPerson.qualification.equals(perseoPerson.qualification)) {

      return false;
    }

    //Stessa sede ...
    return epasPerson.getCurrentOffice().get().perseoId != null
        && perseoPerson.perseoOfficeId != null
        && Objects.equals(epasPerson.getCurrentOffice().get().perseoId, 
            perseoPerson.perseoOfficeId);
  }

  /**
   * Diagnostica della persona.
   */
  public boolean isPersonSynchronized(Person epasPerson, Person perseoPerson,
      List<Contract> perseoContracts) {

    Contract epasActiveContract = null;
    Contract perseoActiveContract = null;

    //Se ha il perseoId popolato allora l'entità di perseo associata è aggiornata
    if (epasPerson.perseoId != null) {
      if (!personEquals(epasPerson, perseoPerson)) {
        return false;
      }
    }

    //Cerco il contratto attivo su perseo... si potrebbe chiedere direttamente a perseo.
    for (Contract perseoContract : perseoContracts) {
      if (DateUtility.isDateIntoInterval(LocalDate.now(), perseoContract.periodInterval())) {
        perseoActiveContract = perseoContract;
        break;
      }
    }

    //Tutti i contratti con perseoId popolato sono sincronizzati
    // Contemporaneamente mi prelevo il contratto attivo epas
    for (Contract epasContract : epasPerson.contracts) {
      if (DateUtility.isDateIntoInterval(LocalDate.now(), epasContract.periodInterval())) {
        epasActiveContract = epasContract;
      }
      if (epasContract.perseoId != null) {
        if (!isContractSynchronized(epasContract, perseoContracts)) {
          return false;
        }
      }
    }

    //Se su perseo è attiva
    if (perseoActiveContract != null) {
      //Ha il perseoId popolato
      if (epasPerson.perseoId == null) {
        return false;
      }
      //Il contratto epas attivo è correttamente sincronizzato
      return epasActiveContract != null && epasActiveContract.perseoId != null;
    }

    return true;
  }

  /**
   * Comparazione contratti epas <-> perseo.
   */
  private boolean contractEquals(Contract epasContract, Contract perseoContract) {

    if (!epasContract.perseoId.equals(perseoContract.perseoId)) {
      return false;
    }

    //Controllo data inizio
    if (!epasContract.beginDate.isEqual(perseoContract.beginDate)) {
      return false;
    }

    //Stesso tipo fine
    if (epasContract.isTemporaryMissing != perseoContract.isTemporaryMissing) {
      return false;
    }

    //Controllo data fine
    if (perseoContract.isTemporaryMissing && perseoContract.calculatedEnd() == null) {
      // Unico caso in cui comanda il dato in epas che è corretto.
      return true;
    }
    if (epasContract.calculatedEnd() == null && perseoContract.calculatedEnd() != null 
        || epasContract.calculatedEnd() != null && perseoContract.calculatedEnd() == null) {
      return false;
    }
        
    return epasContract.calculatedEnd() == null && perseoContract.calculatedEnd() == null
        || epasContract.calculatedEnd().isEqual(perseoContract.calculatedEnd());
  }

  /**
   * Diagnostica del contratto.
   *
   * @param epasContract il contratto da verificare
   * @param perseoContracts i contratti strutturati della persona in perseo.
   * @return esito
   */
  public boolean isContractSynchronized(Contract epasContract, List<Contract> perseoContracts) {

    if (epasContract.perseoId == null) {
      return false;
    }

    for (Contract perseoContract : perseoContracts) {
      if (Objects.equals(perseoContract.perseoId, epasContract.perseoId)) {
        return contractEquals(epasContract, perseoContract);
      }
    }

    return false;
  }


}
