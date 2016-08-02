package dao.wrapper;

import com.google.common.base.Optional;

import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

/**
 * Oggetto persone con molte funzionalità aggiuntive.
 *
 * @author marco
 */
public interface IWrapperPerson extends IWrapperModel<Person> {

  /**
   * Se la persona ha contratto attivo nella data.
   */
  boolean isActiveInDay(LocalDate date);

  /**
   * Se la persona ha contratto attivo nel mese.
   */
  boolean isActiveInMonth(YearMonth yearMonth);

  /**
   * Il contratto attuale. Istanzia una variabile Lazy.
   */
  Optional<Contract> getCurrentContract();


  /**
   * Il piano ferie attuale. Istanzia una variabile Lazy.
   */
  Optional<VacationPeriod> getCurrentVacationPeriod();


  /**
   * Il tipo orario attuale. Istanzia una variabile Lazy.
   */
  Optional<WorkingTimeType> getCurrentWorkingTimeType();

  /**
   * Il periodo del tipo orario attuale. Istanzia una variabile Lazy.
   */
  Optional<ContractWorkingTimeType> getCurrentContractWorkingTimeType();

  /**
   * Il tipo timbratura attuale. Istanzia una variabile Lazy.
   */
  Optional<ContractStampProfile> getCurrentContractStampProfile();

  /**
   * I contratti della persona ordinati per date crescenti.
   */
  List<Contract> orderedContracts();
  
  /**
   * I contratti della persona nell'anno ordinati per date crescenti.
   */
  List<Contract> orderedYearContracts(int year);

  /**
   * I contratti della persona nel mese ordinati per date crescenti.
   */
  List<Contract> orderedMonthContracts(int year, int month);

  /**
   * L'ultimo contratto attivo della persona nel mese.
   */
  Optional<Contract> getLastContractInMonth(int year, int month);

  /**
   * Il primo contratto attivo della persona nel mese.
   */
  Optional<Contract> getFirstContractInMonth(int year, int month);

  /**
   * L'ultimo mese con contratto attivo.
   */
  YearMonth getLastActiveMonth();

  /**
   * True se la persona è passata da determinato a indeterminato durante l'anno.
   */
  public boolean hasPassToIndefiniteInYear(int year);

  /**
   * L'esito dell'invio attestati per la persona (null se non è ancora stato effettuato).
   */
  public CertificatedData getCertificatedData(int year, int month);

  /**
   * Getter per la competenza della persona tramite CompetenceCode, year e month.
   */
  public Competence competence(final CompetenceCode code, final int year, final int month);

  /**
   * Il residuo positivo del mese fatto dalla person.
   */
  public Integer getPositiveResidualInMonth(int year, int month);

  /**
   * Diagnostiche sui dati della persona.
   */
  public boolean currentContractInitializationMissing();

  public boolean currentContractMonthRecapMissing();
  
  /**
   * Diagnostiche sullo stato di sincronizzazione della persona.
   * 
   * Ha perseoId null oppure uno dei suoi contratti attivi o futuri ha perseoId null.
   */
  public boolean isProperSynchronized();
  /**
   * Il contratto della persona con quel perseoId.
   * @param perseoId perseoId
   * @return contratto
   */
  public Contract perseoContract(Long perseoId);
  
  /**
   * 
   * @return true se la persona è un tecnico (liv. IV - VIII), false altrimenti
   */
  public boolean isTechnician();
  

}
