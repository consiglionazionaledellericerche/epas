package manager.services.vacations;

import com.google.common.base.Optional;

import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.Office;
import models.Person;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Servizio ferie e permessi.
 * 
 * @author alessandro
 *
 */
public interface IVacationsService {

  /**
   * Costruisce il riepilogo ferie.
   * @param year anno
   * @param contract contratto
   * @param actualDate data di maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @param otherAbsences altre assenze extra db.
   * @param dateAsToday per simulare oggi con un giorno diverso da oggi
   * @return il recap
   */
  Optional<IVacationsRecap> create(int year, Contract contract,
      LocalDate actualDate, boolean considerExpireLastYear,
      List<Absence> otherAbsences, Optional<LocalDate> dateAsToday);
  
  /**
   * Costruisce il riepilogo ferie con valori di default. 
   * @param year anno
   * @param contract contratto
   * @param actualDate data maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @return il recap
   */
  Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate,
      boolean considerExpireLastYear);
  
  /**
   * Costruisce il riepilogo ferie con valori di default. 
   * @param year anno
   * @param contract contratto
   * @param actualDate data di maturazione
   * @param considerExpireLastYear se considerare la scadenza delle ferie nell'anno.
   * @param dateAsToday per simulare oggi con un giorno diverso da oggi
   * @return il recap
   */
  Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate, 
      boolean considerExpireLastYear, LocalDate dateAsToday);
  
  /**
   * Il primo codice utilizzabile nella data. Ordine: 31, 32, 94.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return tipo assenza
   */
  AbsenceType whichVacationCode(Person person, LocalDate date, List<Absence> otherAbsences);
  
  /**
   * Verifica che la persona alla data possa prendere un giorno di ferie codice 32.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 32.
   */
  boolean canTake32(Person person, LocalDate date, List<Absence> otherAbsences);
  
  /**
   * Verifica che la persona alla data possa prendere un giorno di ferie codice 31.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 31.
   */
  boolean canTake31(Person person, LocalDate date, List<Absence> otherAbsences);
  
  /**
   * Verifica che la persona alla data possa prendere un giorno di permesso con codice 94.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 94.
   */
  boolean canTake94(Person person, LocalDate date, List<Absence> otherAbsences);
  
  /**
   * La data di scadenza delle ferie anno passato per l'office passato come argomento nell'anno.
   * year.
   * @param year anno
   * @param office office
   * @return data expire
   */
  LocalDate vacationsLastYearExpireDate(int year, Office office);
  
  /**
   * Se sono scadute le ferie per l'anno passato.
   * @param year anno
   * @param expireDate data scadenza
   * @return esito 
   */
  boolean isVacationsLastYearExpired(int year, LocalDate expireDate);  
}
