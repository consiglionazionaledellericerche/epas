package controllers;

import java.util.List;
import javax.inject.Inject;
import dao.history.HistoryDao;
import dao.history.HistoryValue;
import models.Competence;
import models.Contract;
import models.PersonDay;
import models.absences.Absence;
import models.base.BaseModel;
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
  
  public static void contractHistory(long contractId) {
    boolean found = false;
    final Contract contract = Contract.findById(contractId);
    if (contract == null) {
      render(found);
    }
    found = true;
    List<HistoryValue<Contract>> historyContract = historyDao.contracts(contractId);
    
    render(historyContract, contract, found);
  }
}
