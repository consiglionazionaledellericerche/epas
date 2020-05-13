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
import helpers.validators.StringIsTime;
import it.cnr.iit.epas.DateUtility;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.TeleworkStampingManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.telework.errors.Errors;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.TeleworkStamping;
import models.dto.TeleworkPersonDayDto;
import models.enumerate.StampTypes;
import play.data.validation.CheckWith;
import play.data.validation.Required;
import play.data.validation.Validation;
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
   * Renderizza il template per l'inserimento e la visualizzazione delle timbrature
   * per telelavoro nell'anno/mese passati come parametro.
   * @param year l'anno
   * @param month il mese
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
    List<StampTypes> beginEnd = StampTypes.beginEndTelework();
    List<StampTypes> meals = StampTypes.beginEndMealInTelework();
    List<StampTypes> interruptions = StampTypes.beginEndInterruptionInTelework();
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
          .beginEnd(manager.getSpecificTeleworkStampings(day.personDay, beginEnd))
          .meal(manager.getSpecificTeleworkStampings(day.personDay, meals))
          .interruptions(manager.getSpecificTeleworkStampings(day.personDay, interruptions))
          .build();
      list.add(dto);
    }
    
    render(list, year, month);
  }
  
  /**
   * Renderizza la modale per l'inserimento della timbratura in telelavoro.
   * @param personId l'identificativo della persona
   * @param date la data in cui inserire la timbratura
   */
  public static void insertStamping(Long personId, LocalDate date) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    Preconditions.checkState(!date.isAfter(LocalDate.now()));
    rules.checkIfPermitted(person);

    TeleworkStamping stamping = new TeleworkStamping();
    render(person, date, stamping);
  }
  
  /**
   * Cancella la timbratura in telelavoro.
   * @param teleworkStampingId l'identificativo della timbratura in telelavoro
   */
  public static void deleteTeleworkStamping(long teleworkStampingId) {
    
  }
  
  /**
   * Persiste la timbratura in telelavoro.
   * @param personId l'identificativo della persona
   * @param date la data 
   * @param stamping la timbratura da salvare
   * @param time l'orario della timbratura
   */
  public static void save(Long personId, @Required LocalDate date, @Required TeleworkStamping stamping,
      @Required @CheckWith(StringIsTime.class) String time) {
    Preconditions.checkState(!date.isAfter(LocalDate.now()));

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    
    PersonDay pd = personDayManager.getOrCreateAndPersistPersonDay(person, date);
    Optional<Errors> check = manager.checkTeleworkStamping(stamping, pd);
    if (check.isPresent()) {
      
    }
  }
  
  public static void editTeleworkStamping(long teleworkStampingId) {
    
  }
  
}
