package controllers;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import lombok.Data;
import manager.PersonManager;
import manager.SecureManager;
import manager.services.absences.AbsenceService;
import manager.services.absences.model.VacationSituation;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With({Resecure.class})
public class MonthRecaps extends Controller {

  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonManager personManager;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;

  /**
   * Controller che gescisce il calcolo del riepilogo annuale residuale delle persone.
   */
  public static void showRecaps(int year, int month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();

    List<Person> simplePersonList = personDao.list(
        Optional.<String>absent(), Sets.newHashSet(office),
        false, monthBegin, monthEnd, false).list();

    List<IWrapperPerson> personList = FluentIterable
        .from(simplePersonList)
        .transform(wrapperFunctionFactory.person()).toList();

    List<ContractMonthRecap> recaps = Lists.newArrayList();

    for (IWrapperPerson person : personList) {

      for (Contract c : person.getValue().contracts) {
        IWrapperContract contract = wrapperFactory.create(c);

        YearMonth yearMonth = new YearMonth(year, month);

        Optional<ContractMonthRecap> recap = contract.getContractMonthRecap(yearMonth);
        if (recap.isPresent()) {
          recaps.add(recap.get());
        } else {
          //System.out.println(person.getValue().fullName());
        }
      }
    }


    render(recaps, year, month, office);
  }

  /**
   * Recap chiesto da IVV. TODO: una raccolta di piccole funzionalità
   *
   * @param year     anno
   * @param month    mese
   * @param officeId sede
   */
  public static void customRecap(final int year, final int month, Long officeId) {

    Set<Office> offices = secureManager
        .officesReadAllowed(Security.getUser().get());
    if (offices.isEmpty()) {
      forbidden();
    }
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    LocalDate monthBegin = new LocalDate().withYear(year)
        .withMonthOfYear(month).withDayOfMonth(1);
    LocalDate monthEnd = new LocalDate().withYear(year)
        .withMonthOfYear(month).dayOfMonth().withMaximumValue();

    List<Person> activePersons = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false, monthBegin, monthEnd, true)
        .list();

    List<CustomRecapDTO> customRecapList = Lists.newArrayList();

    GroupAbsenceType vacationGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    
    for (Person person : activePersons) {

      IWrapperPerson wrPerson = wrapperFactory.create(person);

      for (Contract contract : wrPerson.orderedMonthContracts(year, month)) {

        VacationSituation situation = absenceService
            .buildVacationSituation(contract, year, vacationGroup, Optional.of(monthEnd), false);

        // Danila voleva un riepilogo con data residuale la fine del mese.
        // Per essere danila compliant andrebbero tolte dal conteggio le assenze effettuate 
        // dopo monthEnd. Anche chissene.

        CustomRecapDTO danilaDto = new CustomRecapDTO();
        danilaDto.ferieAnnoCorrente = situation.currentYear.usableTotal();

        danilaDto.ferieAnnoPassato = situation.lastYear != null 
            ? situation.lastYear.usableTotal() : 0;

        danilaDto.permessi = situation.permissions.usableTotal();

        Optional<ContractMonthRecap> recap =
            wrapperFactory.create(contract).getContractMonthRecap(
                new YearMonth(year, month));

        danilaDto.monteOreAnnoPassato = recap.get()
            .remainingMinutesLastYear;
        danilaDto.monteOreAnnoCorrente = recap.get()
            .remainingMinutesCurrentYear;
        danilaDto.giorni = 22 - personManager
            .numberOfCompensatoryRestUntilToday(person,
                year, month);

        danilaDto.straordinariFeriali = recap.get()
            .straordinariMinutiS1Print;
        danilaDto.straordinariFestivi = recap.get()
            .straordinariMinutiS2Print;
        danilaDto.person = person;
        customRecapList.add(danilaDto);
      }

    }
    render(customRecapList, offices, office, year, month);
  }

  /**
   *  Raccoglitore per il CustomRecap.
   *  @author alessandro
   */
  @Data
  public static class CustomRecapDTO {
    public Person person;
    public int ferieAnnoPassato;
    public int ferieAnnoCorrente;
    public int permessi;
    public int monteOreAnnoPassato;
    public int monteOreAnnoCorrente;
    public int giorni;
    public int straordinariFeriali;
    public int straordinariFestivi;
  }

}
