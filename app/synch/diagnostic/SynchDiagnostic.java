package synch.diagnostic;

import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.Institute;
import models.Office;
import models.Person;

import org.assertj.core.util.Maps;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;

/**
 * Mette a disposizione servizi di diagnosi dello stato di sincronizzazione. 
 * 
 * Istituto:
 * Un istituto è correttamente sincronizzato se:
 *  1) Ha il perseoId popolato e l'entità di perseo associata è aggiornata.
 *     TODO: Cds, Sigla, Nome
 *  2) Tutte le sue sedi di epas sono correttamente sincronizzate
 * 
 * Una sede è correttamente sincronizzata se:
 *  1) Ha il perseoId popolato e l'entità di perseo associata è aggiornata.
 *  2) Tutte le persone con matricola in perseo sono caricate in epas e sincronizzate.
 *  
 * Una persona è correttamente sincronizzata se:
 *  1) Se ha il perseoId popolato allora l'entità di perseo associata è aggiornata
 *  2) Tutti i suoi contratti con perseoId popolato sono correttamente sincronizzati
 *  3) Se su perseo è attiva: 
 *    a) Ha il perseoId popolato
 *    b) Il contratto epas attivo è correttamente sincronizzato
 * 
 * Un contratto è correttamente sincronizzato se:
 *  1) Ha il perseoId popolato e l'entità di perseo associata è aggiornata.
 *     Data inizio, Data Fine (con gestione caso determinato)
 * @author alessandro
 *
 */
public class SynchDiagnostic {

  
  /**
   * Comparazione Institute epas <-> perseo.
   * @param epasInstitute
   * @param perseoInstitute
   * @return
   */
  private boolean instituteEquals(Institute epasInstitute, Institute perseoInstitute) {
    
    if (!epasInstitute.cds.equals(perseoInstitute.cds) 
        || !epasInstitute.name.equals(perseoInstitute.name) 
        || !epasInstitute.code.equals(perseoInstitute.code) ) {
      
      return false;
    }
    
    return true;
  }

  
  private boolean isInstituteSynchronized(Institute epasInstitute, Institute perseoInstitute, 
      Map<Integer, Person> perseoPeople) {
    
    if (epasInstitute.perseoId == null) {
      return false;
    }
    if (!instituteEquals(epasInstitute, perseoInstitute)) {
      return false;
    }
    
    for (Office epasOffice : epasInstitute.seats) {
      
      //il perseoOffice
      Office perseoOffice = null;
      for (Office office : perseoInstitute.seats) {
        if (office.perseoId == epasOffice.perseoId) {
          perseoOffice = office;
        }
      }
      if (perseoOffice == null) {
        return false;
      }
      
      //le persone epas con matricola nel epasOffice Map(number -> Person)
      Map<Integer, Person> epasPeople = Maps.newHashMap();
      for (Person person : epasOffice.persons) {
        if (person.number != null) {
          epasPeople.put(person.number, person);
        }
      }
      
      //le persone perseo nel perseoOffice Map(number -> Person)
      //Map<Integer, Person> perseoPeople = null;
      
      //i contratti perseo delle persone Map(number -> List(contract)) //può essere unica...
      Map<Integer, List<Contract>> perseoContracts = null;
      
      if (!isSeatSynchronized(epasOffice, perseoOffice, 
          epasPeople, perseoPeople, perseoContracts)) {
        
      }
    }
    
    return false;
  }
  
  /**
   * Comparazione seat epas <-> perseo.
   * @param epasOffice
   * @param perseoOffice
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
    if (epasOffice.institute.perseoId == null 
        || perseoOffice.institute.perseoId == null 
        || epasOffice.institute.perseoId != perseoOffice.institute.perseoId ) {
      return false;
    }
    
    return true;
  }
  
  /**
   * 
   * @param epasOffice
   * @param perseoOffice
   * @return
   */
  public boolean isSeatSynchronized(Office epasOffice, Office perseoOffice, 
      Map<Integer, Person> epasPeople, Map<Integer, Person> perseoPeople, 
      Map<Integer, List<Contract>> perseoContracts) {
    
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
   * @param epasPerson
   * @param perseoPerson
   * @return
   */
  public boolean personEquals(Person epasPerson, Person perseoPerson) {
    
    if (epasPerson.qualification == null) {
      return false;
    }
    
    if (!epasPerson.name.equals(perseoPerson.name) 
        || !epasPerson.surname.equals(perseoPerson.surname) 
        || !epasPerson.number.equals(perseoPerson.number)
        || !epasPerson.qualification.equals(perseoPerson.qualification) ) {

      return false;
    }
    
    //Stessa sede ...
    if (epasPerson.office.perseoId == null 
        || perseoPerson.perseoOfficeId == null 
        || epasPerson.office.perseoId != perseoPerson.perseoOfficeId ) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Diagnostica della persona.
   * 
   * @param epasPerson
   * @param perseoPerson
   * @param perseoContracts
   * @return
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
    for(Contract epasContract : epasPerson.contracts) {
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
      if (epasActiveContract == null || epasActiveContract.perseoId == null) {
        return false;
      }
    }
   
    return true;
  }
  
  /**
   * Comparazione contratti epas <-> perseo.
   * @param epasContract
   * @param perseoContract
   * @return
   */
  private boolean contractEquals(Contract epasContract, Contract perseoContract) {
    
    if (epasContract.perseoId != perseoContract.perseoId) {
      return false;
    }
    
    //Controllo data inizio
    if (!epasContract.beginDate.isEqual(perseoContract.beginDate)) {
      return false;
    }

    //Stesso tipo fine
    if (epasContract.isTemporary != perseoContract.isTemporary) {
      return false;
    }
    
    //Controllo data fine
    if (perseoContract.isTemporary && perseoContract.calculatedEnd() == null) {
      // Unico caso in cui comanda il dato in epas che è corretto.
      return true;
    }
    if (!epasContract.calculatedEnd().isEqual(perseoContract.calculatedEnd())) {
      return false;
    }
    return true;
  }
  /**
   * Diagnostica del contratto.
   *   
   * @param contract il contratto da verificare 
   * @param perseoContracts i contratti strutturati della persona in perseo.
   * @return esito
   */
  public boolean isContractSynchronized(Contract epasContract, List<Contract> perseoContracts) {
    
    if (epasContract.perseoId == null) {
      return false;
    }
    
    for (Contract perseoContract : perseoContracts) {
      if (perseoContract.perseoId == epasContract.perseoId) { 
        return contractEquals(epasContract, perseoContract);
      }
    }
    
    return false;
  }
  
  
}
