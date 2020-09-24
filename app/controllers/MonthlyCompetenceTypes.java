package controllers;

import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import dao.CompetenceCodeDao;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.MonthlyCompetenceType;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
@Slf4j
public class MonthlyCompetenceTypes extends Controller {
  
  @Inject
  private static CompetenceCodeDao competenceCodeDao;

  /**
   * 
   */
  public static void insertMonthlyCompetenceType() {
    
    MonthlyCompetenceType monthlyCompetencetype = new MonthlyCompetenceType();
    List<CompetenceCode> competenceCodeList = competenceCodeDao.getAllCompetenceCode();
        
    render(monthlyCompetencetype, competenceCodeList);
  }
  
  /**
   * 
   * @param monthlyCompetencetype
   */
  public static void saveType(MonthlyCompetenceType monthlyCompetencetype) {
    monthlyCompetencetype.save();
  }
}
