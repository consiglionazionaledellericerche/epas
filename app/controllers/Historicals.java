package controllers;

import java.util.List;
import javax.inject.Inject;
import dao.history.CompetenceHistoryDao;
import dao.history.HistoryValue;
import models.Competence;
import models.PersonDay;
import models.absences.Absence;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class Historicals extends Controller {
  
  @Inject
  static CompetenceHistoryDao competenceHistoryDao;

  public static void competenceHistory(long competenceId) {
    boolean found = false;
    final Competence competence = Competence.findById(competenceId);
    if (competence == null) {

      render(found);
    }
    found = true;
    List<HistoryValue<Competence>> historyCompetence = competenceHistoryDao
        .competences(competenceId);
    
    render(historyCompetence, competence, found);
  }
}
