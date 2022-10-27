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

package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import dao.PersonMonthRecapDao;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Person;
import models.PersonMonthRecap;
import org.joda.time.LocalDate;

/**
 * Manager per la gestione dei PersonMonth.
 */
public class PersonMonthsManager {

  private final PersonMonthRecapDao personMonthRecapDao;  

  /**
   * Costruttore per l'injection.
   */
  @Inject
  public PersonMonthsManager(PersonMonthRecapDao personMonthRecapDao) {
    this.personMonthRecapDao = personMonthRecapDao;
  }

  /**
   * Salva le ore di formazione per il periodo specificato.
   *
   * @param approved se sono già approvate o no le ore di formazione
   * @param value la quantità di ore di formazione da approvare
   * @param begin data inizio del periodo di formazione
   * @param end data fine del periodo di formazione.
   */
  public void saveTrainingHours(Person person, Integer year, Integer month, Integer begin, 
        Integer end, boolean approved, Integer value) {
    PersonMonthRecap pm = new PersonMonthRecap(person, year, month);
    LocalDate beginDate = new LocalDate(year, month, begin);
    pm.setHoursApproved(false);
    pm.setTrainingHours(value);
    pm.setFromDate(beginDate);
    pm.setToDate(new LocalDate(year, month, end));
    pm.save();
  }

  /**
   * Un insertable che controlla se posso inserire o meno un certo periodo di formazione.
   *
   * @param begin data inizio
   * @param end data fine
   * @param value la quantità da inserire
   * @param beginDate la data da quando inserire
   * @param endDate la data fino a cui inserire
   * @return un Insertable che controlla se è possibile prendere i parametri passati alla funzione
   *     oppure se questi presentano dei problemi.
   */
  public Insertable checkIfInsertable(
      int begin, int end, Integer value, LocalDate beginDate, LocalDate endDate) {
    Insertable rr = new Insertable(true, "");
    if (begin > end) {
      rr.message =
          "La data di inizio del periodo di formazione non può essere successiva a quella di fine";
      rr.result = false;
    }
    if (value == null || value < 0
        || value > 24 * (endDate.getDayOfMonth() - beginDate.getDayOfMonth() + 1)) {
      rr.message =
          "Non sono valide le ore di formazione negative, testuali o che superino la quantità "
              + "massima di ore nell'intervallo temporale inserito.";
      rr.result = false;
    }
    return rr;
  }

  /**
   * Un insertable che controlla se per la persona è possibile inserire un periodo di formazione.
   *
   * @param person la persona da controllare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param beginDate la data di inizio
   * @param endDate la data di fine
   * @return un Insertable che verifica se esiste già un periodo contenente delle ore di formazione
   *     per la persona person.
   */
  public Insertable checkIfPeriodAlreadyExists(Person person, int year, int month,
      LocalDate beginDate, LocalDate endDate) {

    List<PersonMonthRecap> pmList = personMonthRecapDao
        .getPersonMonthRecaps(person, year, month, beginDate, endDate);

    Insertable rr = new Insertable(true, "");

    if (pmList != null && pmList.size() > 0) {
      rr.message = "Esiste un periodo di ore di formazione "
          + "che contiene uno o entrambi i giorni specificati.";
      rr.result = false;

    }
    return rr;
  }

  /**
   * Un insertable che controlla se il periodo di formazione è già stato mandato o no.
   *
   * @param person la persona da controllare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return un Insertable che verifica se le ore di formazione per anno e mese richieste sono già
   *     state inviate.
   */
  public Insertable checkIfAlreadySent(Person person, int year, int month) {
    Insertable rr = new Insertable(true, "");
    List<PersonMonthRecap> list = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year,
            Optional.fromNullable(month), Optional.fromNullable(Boolean.TRUE));

    // TODO & FIXME: lo stato di validazione deve essere intercettato da attestati.
    
    if (list.size() > 0) {
      rr.message =
          "Impossibile inserire ore di formazione per il mese precedente poichè gli "
              + "attestati per quel mese sono già stati inviati";
      rr.result = false;

    }
    return rr;
  }

  /**
   * Un insertable che controlla se esiste già un personMonthRecap per la persona.
   *
   * @param pm il personMonthRecap relativo alla formazione della persona
   * @return un Insertable che controlla se esiste nel database una entry con l'id passato come
   *     parametro per quelle ore di formazione.
   */
  public Insertable checkIfExist(PersonMonthRecap pm) {
    Insertable rr = new Insertable(true, "");

    if (pm == null) {
      rr.message = "Ore di formazione non trovate. Operazione annullata.";
      rr.result = false;
    }
    return rr;
  }

  /**
   * DTO per rappresenta se un periodo di formazione è inseribile o meno e l'eventualmente 
   * motivazione.
   */
  public static final class Insertable {
    private boolean result;
    private String message;

    /**
     * Costruttore.
     *
     * @param result il risultato dell'inserimento
     * @param message il messaggio
     */
    public Insertable(boolean result, String message) {
      this.result = result;
      this.message = message;

    }

    public String getMessage() {
      return this.message;
    }

    public boolean getResult() {
      return this.result;
    }
  }

  /**
   * Ritorna la mappa persona-personMonthRecap per l'anno/mese della lista di persone.
   *
   * @param personList la lista di persone
   * @param year l'anno
   * @param month il mese
   * @return la mappa contenente per ogni persona la propria situazione in termini di ore di
   *     formazione approvate o no.
   */
  public Map<Person, List<PersonMonthRecap>> createMap(
      List<Person> personList, int year, int month) {

    Map<Person, List<PersonMonthRecap>> map = Maps.newHashMap();
    for (Person person : personList) {

      List<PersonMonthRecap> pmrList = personMonthRecapDao
          .getPersonMonthRecapInYearOrWithMoreDetails(person, year, 
              Optional.fromNullable(month), Optional.<Boolean>absent());
      if (!pmrList.isEmpty()) {
        map.put(person, pmrList);
      }      
    }
    return map;
  }

}