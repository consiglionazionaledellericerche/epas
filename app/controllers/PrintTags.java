package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import it.cnr.iit.epas.DateUtility;

import manager.SecureManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingRecap;
import manager.recaps.personStamping.PersonStampingRecapFactory;

import models.Person;
import models.Stamping;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import static play.modules.pdf.PDF.renderPDF;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class PrintTags extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static SecureManager secureManager;
  @Inject
  static StampingHistoryDao stampingHistoryDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static IWrapperFactory wrapperFactory;

  public static void showTag(Person person, int month, int year, boolean includeStampingDetails) {

    if (person == null) {
      flash.error("Selezionare una persona dall'elenco del personale.");
      listPersonForPrintTags(year, month);
    }

    rules.checkIfPermitted(person);

    PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);

    String titolo = "Situazione presenze mensile "
        + DateUtility.fromIntToStringMonth(month) + " " + year + " di "
        + person.surname + " " + person.name;

    List<PersonStampingDayRecap> days = psDto.daysRecap;
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();

    for (PersonStampingDayRecap day : days) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.stampings) {
          if (stamping.markedByAdmin) {
            historyStampingsList.add(stampingHistoryDao.stampings(stamping.id));
          }
        }
      }
    }

    renderPDF(psDto, titolo, includeStampingDetails, historyStampingsList);
  }

  public static void listPersonForPrintTags(int year, int month) {

    LocalDate date = new LocalDate(year, month, 1);

    List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        false, date, date.dayOfMonth().withMaximumValue(), true).list();

    render(personList, date, year, month);
  }

  public static void showPersonTag(Integer year, Integer month) {

    Optional<User> currentUser = Security.getUser();
    Preconditions.checkState(currentUser.isPresent());
    Preconditions.checkNotNull(currentUser.get().person);

    IWrapperPerson person = wrapperFactory
        .create(Security.getUser().get().person);


    if (!person.isActiveInMonth(new YearMonth(year, month))) {

      flash.error("La persona %s non ha contratto attivo nel mese selezionato",
          person.getValue().fullName());
      render("@redirectToIndex");
    }

    PersonStampingRecap psDto = stampingsRecapFactory
        .create(person.getValue(), year, month);

    // FIXME: spostare nel template
    String titolo = "Situazione presenze mensile "
        + DateUtility.fromIntToStringMonth(month) + " " + year + " di "
        + person.getValue().fullName();

    renderPDF(psDto, titolo);

  }

}
