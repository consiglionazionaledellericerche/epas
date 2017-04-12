package dao.wrapper;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateInterval;

import models.Contract;
import models.ContractMonthRecap;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

public interface IWrapperContract extends IWrapperModel<Contract> {

  /**
   * True se il contratto è l'ultimo contratto della persona per mese e anno selezionati.
   * @param month mese
   * @param year anno
   * @return esito.
   */
  boolean isLastInMonth(int month, int year);

  /**
   * True se il contratto è a tempo determinato.
   * @return esito.
   */
  boolean isDefined();

  /**
   * L'intervallo effettivo per il contratto. Se endContract (data terminazione del contratto) 
   * è presente sovrascrive contract.endDate.
   * @return l'intervallo.
   */
  DateInterval getContractDateInterval();

  /**
   * L'intervallo ePAS per il contratto. Se è presente una data di inizializzazione generale del
   * contratto sovrascrive contract.beginDate.
   * @return l'intervallo.
   */
  DateInterval getContractDatabaseInterval();

  /**
   * L'intervallo ePAS per il contratto dal punto di vista dei buoni pasto. Se è presente una data 
   * di inizializzazione buoni pasto del contratto sovrascrive contract.beginDate.
   * @return l'intervallo.
   */
  DateInterval getContractDatabaseIntervalForMealTicket();

  /**
   * Il riepilogo mensile attualmente persistito (se esiste) per il mese passato come parametro.
   * @param yearMonth mese
   * @return il riepilogo (absent se non presente)
   */
  Optional<ContractMonthRecap> getContractMonthRecap(YearMonth yearMonth); 
  
  /**
   * Il mese del primo riepilogo esistente per il contratto. absent() se non ci sono i dati per
   * costruire il primo riepilogo.
   * @return il mese (absent se non esiste).
   */
  Optional<YearMonth> getFirstMonthToRecap();

  /**
   * Il mese dell'ultimo riepilogo esistente per il contratto (al momento della chiamata).<br>
   * Il mese attuale se il contratto termina dopo di esso. Altrimenti il mese di fine contratto.
   * @return il mese
   */
  YearMonth getLastMonthToRecap();
  
  /**
   * Se il contratto è stato inizializzato per la parte residuale nel mese passato come argomento. 
   * @param yearMonth mese
   * @return esito
   */
  boolean residualInitInYearMonth(YearMonth yearMonth);

  //List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList();

  // ##############################################################################################
  // Strumenti di Diagnosi del contratto.
  // ##############################################################################################
  
  /**
   * Se per il contratto c'è almeno un riepilogo necessario non persistito.
   * @return esito.
   */
  public boolean monthRecapMissing();
  
  /**
   * Se un riepilogo per il mese passato come argomento non è persistito.
   * @param yearMonth mese
   * @return esito.
   */
  public boolean monthRecapMissing(YearMonth yearMonth);
  
  /**
   * La data di inizializzazione è la successiva fra la creazione della persona e l'inizio utilizzo
   * del software della sede della persona (che potrebbe cambiare a causa del trasferimento).
   * @return data
   */
  public LocalDate dateForInitialization();
  
  /**
   * La data di inizializzazione per i buoni pasto. Non è mandatoria, ma è ignorata se precedente
   * l'inizializzazione del contratto.
   * @return data
   */
  public LocalDate dateForMealInitialization();
  
  /**
   * Se il contratto è finito prima che la sede della persona fosse installata in ePAS.
   * @return esito
   */
  public boolean noRelevant();

  /**
   * Se il contratto necessita di inizializzazione. 
   * @return esito
   */
  public boolean initializationMissing();
  
  /**
   * Se sono state definite sia la inizializzazione generale che quella buoni pasto e quella 
   * dei buoni pasto è precedente a quella generale.
   * @return esito
   */
  public boolean mealTicketInitBeforeGeneralInit();

  /**
   * Se il contratto ha tutti i riepiloghi necessari per calcolare il riepilogo per l'anno
   * passato come parametro.
   * @param yearToRecap anno
   * @return esito
   */
  public boolean hasMonthRecapForVacationsRecap(int yearToRecap);

}
