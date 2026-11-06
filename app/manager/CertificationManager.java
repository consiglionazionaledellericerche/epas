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
import dao.PersonDayDao;
import helpers.CacheValues;
import helpers.rest.ApiRequestException;
import it.cnr.iit.epas.DateUtility;
import java.io.File;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.attestati.service.CertificationsComunication;
import manager.attestati.service.PersonCertData;
import models.Office;
import models.Person;
import org.joda.time.YearMonth;


/**
 * Manager per le certification (attestati mensili).
 *
 * @author Alessandro Martelli
 */
@Slf4j
public class CertificationManager {
  
  private PersonDayManager personDayManager;
  private PersonDayDao personDayDao;
  private CacheValues cacheValues;
  private CertificationsComunication certificationsComunication;
  
  /**
   * Construttore di default per l'injection.
   */
  @Inject
  public CertificationManager(PersonDayManager personDayManager, PersonDayDao personDayDao,
      CacheValues cacheValues, CertificationsComunication certificationsComunication) {
    this.personDayDao = personDayDao;
    this.personDayManager = personDayManager;
    this.cacheValues = cacheValues;
    this.certificationsComunication = certificationsComunication;
  }

  /**
   * Stato di validazione dell'attestato mensile della persona, letto direttamente
   * da Attestati.
   * <p>
   * A differenza di {@link #getPersonCertData(Person, Integer, Integer)} non calcola
   * la situazione mensile in ePAS, non confronta i dati e non scrive sul database:
   * effettua solo la chiamata ad Attestati e legge il flag di validazione.
   * </p>
   *
   * @param person la persona
   * @param year l'anno
   * @param month il mese
   * @return true/false se Attestati ha risposto, absent se non è stato possibile
   *     scaricare l'attestato della persona.
   * @throws ExecutionException se non è possibile ottenere il token OAuth per Attestati.
   */
  public Optional<Boolean> getPersonMonthValidated(Person person, int year, int month)
      throws ExecutionException {
    val seatCertification =
        certificationsComunication.getPersonSeatCertification(person, month, year);
    if (!seatCertification.isPresent()) {
      return Optional.absent();
    }
    return Optional.of(seatCertification.get().dipendenti.get(0).validato);
  }

  /**
   * Matricole dei dipendenti della sede con l'attestato mensile validato, lette da Attestati
   * con un'unica chiamata per tutta la sede.
   *
   * @param office la sede
   * @param year l'anno
   * @param month il mese
   * @return le matricole (senza zeri iniziali) dei dipendenti con l'attestato validato.
   * @throws ExecutionException se non è possibile ottenere il token OAuth per Attestati.
   * @throws ApiRequestException se Attestati non risponde correttamente o la risposta non
   *     contiene la lista dei dipendenti con le matricole.
   */
  public Set<String> getOfficeMonthValidatedNumbers(Office office, int year, int month)
      throws ExecutionException {
    val statoValidazione =
        certificationsComunication.getStatoValidazioneDipendenti(office, year, month);
    if (statoValidazione == null || statoValidazione.dipendenti == null) {
      throw new ApiRequestException("Risposta di attestati senza la lista dei dipendenti");
    }
    // Una lista vuota è valida (es. stralcio non ancora fatto), ma se ci sono dipendenti
    // e nessuno ha la matricola il formato della risposta non è quello atteso.
    if (!statoValidazione.dipendenti.isEmpty() && statoValidazione.dipendenti.stream()
        .allMatch(dipendente -> dipendente == null || dipendente.matricola == null)) {
      throw new ApiRequestException("Risposta di attestati con dipendenti senza matricola");
    }
    return statoValidazione.dipendenti.stream()
        .filter(dipendente -> dipendente != null && dipendente.validato
            && dipendente.matricola != null)
        .map(dipendente -> normalizeNumber(dipendente.matricola))
        .collect(Collectors.toSet());
  }

  /**
   * Normalizza la matricola togliendo gli zeri iniziali, perché Attestati la restituisce
   * come numero.
   *
   * @param number la matricola
   * @return la matricola senza zeri iniziali.
   */
  public static String normalizeNumber(String number) {
    return number.trim().replaceFirst("^0+(?=.)", "");
  }
  
  /**
   * Informazioni sui dati mensili inviati ad Attestati prelevati
   * da attestati stesso.
   */
  public PersonCertData getPersonCertData(Person person, Integer year, Integer month) {
    final Map.Entry<Person, YearMonth> cacheKey = new AbstractMap
        .SimpleEntry<>(person, new YearMonth(year, month));
    try {
      return cacheValues.personStatus.get(cacheKey);
    } catch (ExecutionException ex) {
      log.error("Impossibile recuperare la percentuale di avanzamento per la persona {}",
          person.getFullname(), ex);
      throw new RuntimeException(
          String.format("Impossibile recuperare la percentuale di avanzamento per la persona %s",
          person.getFullname()));
    }
    
  }
  
  /**
   * Metodo che genera il file .csv con le informazioni da esportare per i buoni pasto.
   *
   * @param people la lista di persone di cui cercare i buoni pasto
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return il file .csv contenente la lista di persone associate alla propria matricola 
   *     e al numero di buoni pasto maturati nell'anno/mese di riferimento
   */
  public File createFile(List<Person> people, int year, int month) {
    final String yearMonth = "" + year + "-" + DateUtility.checkMonth(month);
    File file = new File("Situazione_mensile buoni pasto " 
        + DateUtility.fromIntToStringMonth(month) + "-" + year + ".csv");
    try {
      PrintWriter pw = new PrintWriter(new File(file.getAbsolutePath()));
      StringBuilder sb = new StringBuilder();
      for (Person person : people) {
        log.debug("Controllo la situazione dei buoni pasto per {}", person.getFullname());
        val mealTicket = personDayManager.numberOfMealTicketToUse(personDayDao
            .getPersonDayInMonth(person, new YearMonth(year, month)));
        if (mealTicket > 0) {
          sb.append(person.getFullname());
          sb.append(";");
          sb.append(fillNumber(person.getNumber()));
          sb.append(";");          
          sb.append(String.valueOf(mealTicket));
          sb.append(";");
          sb.append(yearMonth);
          sb.append("\r\n");
        } else {
          log.debug("Non ci sono buoni da inserire in questo mese per {}", person.getFullname());
        }   
        log.debug("Inseriti {} buoni per {}", mealTicket, person.getFullname());
      }
      pw.write(sb.toString());
      pw.close();
    } catch (Exception ex) {
      log.error("Error in CsvFileWriter !!!");
      ex.printStackTrace();
    }
    
    return file;
  }
  
  /**
   * Metodo privato di normalizzazione della matricola a 6 caratteri.
   *
   * @param number la matricola da normalizzare
   * @return la matricola addizionata di "0" in cima per normalizzarla a 6 cifre.
   */
  private String fillNumber(String number) {
    final int dimension = 6;
    if (number.length() < 6) {
      while (number.length() < dimension) {
        number = "0" + number;        
      }
    }
    return number;
  }
}
