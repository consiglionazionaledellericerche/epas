package controllers;

import com.google.common.collect.Lists;
import dao.history.HistoricalDao;
import dao.history.ContractHistoryDao;
import dao.history.HistoryValue;
import java.util.List;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
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
  static HistoricalDao historicalDao;

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
  
  public static void contractHistory(long contractId, HistorycalType type) {
    boolean found = false;
    final Contract contract = Contract.findById(contractId);
    if (contract == null) {
      render(found);
    }
    found = true;
    List<HistoryValue<Contract>> historyContract = contractHistoryDao.contracts(contractId);
    List<HistoryValue<Contract>> contractModifications = Lists.newArrayList();
    List<HistoryValue<Contract>> initializationModifications = Lists.newArrayList();
    Contract temp = contract;
    if (type.equals(HistorycalType.CONTRACT)) {
      for (HistoryValue<Contract> story : historyContract) {
        //val con = historicalDao.valueAtRevision(Contract.class, contractId, story.revision.id);
        Contract con = story.value;
        log.info("Contract id = {}, revision = {}, perseoId = {}, "
            + "beginDate = {}, endDate = {}, endContract = {}", 
            contract.id, story.revision.id, con.perseoId, con.beginDate, con.endDate, 
            con.endContract);        
        if (!temp.beginDate.isEqual(con.beginDate) 
//            || !temp.endDate.equals(con.endDate)
//            || !temp.endContract.equals(con.endContract) || temp.onCertificate != con.onCertificate
//            || temp.getPreviousContract().equals(con.getPreviousContract())
            ) {
          contractModifications.add(story);
        }
        temp = con;      
      }
      render(contractModifications, contract, found);
    } else {
      //Per l'inizializzazioni...
    }
    
    
    render(historyContract, contract, found);
  }
}
