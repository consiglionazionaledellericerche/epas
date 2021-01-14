package controllers;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import java.util.List;
import javax.inject.Inject;
import dao.history.HistoryDao;
import dao.history.HistoryValue;
import models.Competence;
import models.Contract;
import models.PersonDay;
import models.absences.Absence;
import models.base.BaseModel;
import models.enumerate.HistorycalType;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class Historicals extends Controller {
  
  @Inject
  static HistoryDao historyDao;

  public static void competenceHistory(long competenceId) {
    boolean found = false;
    final Competence competence = Competence.findById(competenceId);
    if (competence == null) {

      render(found);
    }
    found = true;
    List<HistoryValue<Competence>> historyCompetence = historyDao
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
    List<HistoryValue<Contract>> historyContract = historyDao.contracts(contractId);
    List<HistoryValue<Contract>> contractModifications = Lists.newArrayList();
    List<HistoryValue<Contract>> initializationModifications = Lists.newArrayList();
    Contract temp = contract;
    if (type.equals(HistorycalType.CONTRACT)) {
      for (HistoryValue story : historyContract) {
        
        Contract con = (Contract)story.value;
        if (!temp.beginDate.isEqual(con.beginDate) || !temp.endDate.equals(con.endDate)
            || !temp.endContract.equals(con.endContract) || temp.onCertificate != con.onCertificate
            || temp.getPreviousContract().equals(con.getPreviousContract())) {
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
