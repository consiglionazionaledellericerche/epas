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
   * Costruisce il riepilogo ferie con il calcolo di assenze maturate e residue a oggi considerando
   * la data di scadenza ferie anno passato della sede competente. 
   * @param year anno
   * @param contract contratto
   * @return il recap
   */
  Optional<VacationsRecap> create(int year, Contract contract);
  
  /**
   * Costruisce il riepilogo ferie alla fine del mese con il calcolo delle assenze 
   * maturate e residue fino a quel momento (ignorando le eventuali assenze prese successivamente).
   * Serve a Danila.
   * @param year anno
   * @param month anno
   * @param contract contratto
   * @return il recap
   */
  Optional<VacationsRecap> createEndMonth(int year, int month, Contract contract);
  
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
   * Verifica che la persona alla data possa prendere un giorno di ferie con codice 37.
   * @param person persona
   * @param date data 
   * @param otherAbsences altre assenze da considerare.
   * @return true se è possibile prendere il codice 37.
   */
  public boolean canTake37(Person person, LocalDate date, List<Absence> otherAbsences);
  
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
