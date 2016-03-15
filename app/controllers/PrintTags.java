package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.PersonDao;
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;

import manager.SecureManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.Person;
import models.Stamping;

import org.joda.time.LocalDate;

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
  static IWrapperFactory wrapperFactory;

  public static void showTag(Person person, int month, int year, boolean includeStampingDetails) {

    if (person == null) {
      flash.error("Selezionare una persona dall'elenco del personale.");
      listPersonForPrintTags(year, month);
    }

    rules.checkIfPermitted(person);

    PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);

    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
    if (includeStampingDetails) {
      for (PersonStampingDayRecap day : psDto.daysRecap) {
        if (!day.ignoreDay) {
          for (Stamping stamping : day.personDay.stampings) {
            if (stamping.markedByAdmin) {
              historyStampingsList.add(stampingHistoryDao.stampings(stamping.id));
            }
          }
        }
      }
    }

    renderPDF(psDto, includeStampingDetails, historyStampingsList);
  }

  public static void listPersonForPrintTags(int year, int month) {

    LocalDate date = new LocalDate(year, month, 1);

    List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        false, date, date.dayOfMonth().withMaximumValue(), true).list();

    render(personList, date, year, month);
  }
}
