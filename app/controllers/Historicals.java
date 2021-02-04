package controllers;

import com.google.common.collect.Lists;
import dao.history.ContractHistoryDao;
import dao.history.HistoricalDao;
import dao.history.HistoryValue;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.HistoricalManager;
import models.Competence;
import models.Contract;
import models.enumerate.HistorycalType;
import play.mvc.Controller;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class Historicals extends Controller {
  
  @Inject
  static ContractHistoryDao contractHistoryDao;
  @Inject
  static HistoricalDao historicalDao;
  @Inject
  static HistoricalManager historicalManager;

  /**
   * Ritorna lo storico sulle modifiche ad una competenza.
   * @param competenceId l'id della competenza di cui cercare lo storico
   */
  public static void competenceHistory(long competenceId) {
    boolean found = false;
    final Competence competence = Competence.findById(competenceId);
    if (competence == null) {

      render(found);
    }
    found = true;
    List<HistoryValue<Competence>> historyCompetence = contractHistoryDao
        .competences(competenceId);
    
    render(historyCompetence, competence, found);
  }
  
  /**
   * Ritorna lo storico del contratto filtrato per tipologia: contratto/inizializzazione.
   * @param contractId l'id del contratto di cui cercare lo storico
   * @param type la tipologia di informazioni che si richiedono: contratto/inizializzazione
   */
  public static void contractHistory(long contractId, HistorycalType type) {
    boolean found = false;
    final Contract contract = Contract.findById(contractId);
    if (contract == null) {
      render(found);
      return;
    }
    found = true;
    List<HistoryValue<Contract>> historyContract = contractHistoryDao.contracts(contractId);
    List<HistoryValue<Contract>> contractModifications = Lists.newArrayList();
    List<HistoryValue<Contract>> initializationModifications = Lists.newArrayList();
    Contract temp = contract;
    if (type.equals(HistorycalType.CONTRACT)) {
      for (HistoryValue<Contract> story : historyContract) {
        
        Contract con = story.value;
        log.debug("Contract id = {}, revision = {}, perseoId = {}, "
            + "beginDate = {}, endDate = {}, endContract = {}", 
            contract.id, story.revision.id, con.perseoId, con.beginDate, con.endDate, 
            con.endContract);  
        if (con.beginDate == null && con.endDate == null && con.endContract == null) {
          log.debug("Storico con dati incompleti per {}", contract.person.getFullname());
          continue;
        }
        if (!historicalManager.checkDates(temp.beginDate, con.beginDate)
            || !historicalManager.checkDates(temp.endDate, con.endDate)
            || !historicalManager.checkDates(temp.endContract, con.endContract) 
            || temp.onCertificate != con.onCertificate
            || !historicalManager.checkObjects(temp, con)) {
          contractModifications.add(story);
        }
        temp = con;      
      }
      render(contractModifications, contract, found);
    } else {
      for (HistoryValue<Contract> story : historyContract) {
        Contract con = story.value;
        if (!historicalManager.checkDates(temp.sourceDateResidual, con.sourceDateResidual)
            || temp.sourceRemainingMinutesLastYear != con.sourceRemainingMinutesLastYear
            || temp.sourceRemainingMinutesCurrentYear != con.sourceRemainingMinutesCurrentYear) {
          
          initializationModifications.add(story);
        }
        temp = con;
      }
      render("@initializationHistory", initializationModifications, contract, found);
    }    
    
  }
}
