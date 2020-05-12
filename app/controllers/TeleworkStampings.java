package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateUtility;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.TeleworkStampingManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Person;
import models.PersonDay;
import models.TeleworkStamping;
import models.dto.TeleworkPersonDayDto;
import models.enumerate.StampTypes;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class})
public class TeleworkStampings extends Controller{

  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static TeleworkStampingManager manager;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static PersonDayManager personDayManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;
  /**
   * 
   * @param year
   * @param month
   */
  public static void teleworkStampings(final Integer year, final Integer month) {
    if (year == null || month == null) {
      Stampings.stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    }
    val currentPerson = Security.getUser().get().person;
    //Accesso da utente di sistema senza persona associata
    if (currentPerson == null) {
      Application.index();
    }
    List<TeleworkPersonDayDto> list = Lists.newArrayList();
    IWrapperPerson wrperson = wrapperFactory.create(currentPerson);

    if (!wrperson.isActiveInMonth(new YearMonth(year, month))) {
      flash.error("Non esiste situazione mensile per il mese di %s %s",
          DateUtility.fromIntToStringMonth(month), year);

      YearMonth last = wrperson.getLastActiveMonth();
      Stampings.stampings(last.getYear(), last.getMonthOfYear());
    }
    PersonStampingRecap psDto = stampingsRecapFactory
        .create(wrperson.getValue(), year, month, true);
    for (PersonStampingDayRecap day : psDto.daysRecap) {
      
      
      TeleworkPersonDayDto dto = TeleworkPersonDayDto.builder()
          .personDay(day.personDay)
          .beginEnd(manager.getSpecificTeleworkStampings(day.personDay, Optional.<StampTypes>absent(), false))
          .meal(manager.getSpecificTeleworkStampings(day.personDay, Optional.of(StampTypes.PAUSA_PRANZO), false))
          .interruptions(manager.getSpecificTeleworkStampings(day.personDay, Optional.<StampTypes>absent(), true))
          .build();
      list.add(dto);
    }
    
    render(list, year, month);
  }
  
  public static void insertStamping(Long personId, LocalDate date) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    Preconditions.checkState(!date.isAfter(LocalDate.now()));
    rules.checkIfPermitted(person);

    TeleworkStamping stamping = new TeleworkStamping();
    render(person, date, stamping);
  }
  
  public static void deleteTeleworkStamping(long teleworkStampingId) {
    
  }
  
  public static void save() {
    
  }
  
  public static void editTeleworkStamping(long teleworkStampingId) {
    
  }
  
}
