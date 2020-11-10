package controllers;

import dao.CompetenceCodeDao;
import helpers.Web;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.MonthlyCompetenceType;
import org.joda.time.LocalDate;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
@Slf4j
public class MonthlyCompetenceTypes extends Controller {
  
  @Inject
  private static CompetenceCodeDao competenceCodeDao;

  /**
   * Genera la form di creazione di un monthlyCompetenceType.
   */
  public static void insertMonthlyCompetenceType() {
    
    MonthlyCompetenceType monthlyCompetencetype = new MonthlyCompetenceType();
    List<CompetenceCode> competenceCodeList = competenceCodeDao.getAllCompetenceCode();
        
    render(monthlyCompetencetype, competenceCodeList);
  }
  
  /**
   * Persiste il monthlyCompetenceType.
   * @param monthlyCompetencetype l'oggetto da persistere
   */
  public static void saveType(MonthlyCompetenceType monthlyCompetencetype) {
    
    if (Validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());
      render("@insertMonthlyCompetenceType", monthlyCompetencetype);      
    }
    
    monthlyCompetencetype.save();
    flash.success(String.format("Codice %s aggiunto con successo", monthlyCompetencetype.name));

    Competences.manageCompetenceCode();
  }
}
