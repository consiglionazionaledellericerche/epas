package controllers;

import static play.modules.pdf.PDF.renderPDF;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import cnr.sync.dto.DayRecap;

import dao.OfficeDao;
import dao.PersonDao;
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;

import java.util.List;

import javax.inject.Inject;

import manager.PrintTagsManager;
import manager.SecureManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;

import models.Office;
import models.Person;
import models.Stamping;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType;
import models.dto.PrintTagsInfo;
import models.dto.ShiftEvent;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

@With({Resecure.class})
public class PrintTags extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static SecureManager secureManager;
  @Inject
  static PrintTagsManager printTagsManager;
  @Inject
  static IWrapperFactory wrapperFactory;

  /**
   * stampa un file pdf contenente la situazione mensile della persona selezionata.
   * @param person la persona selezionata
   * @param month il mese di interesse
   * @param year l'anno di interesse
   * @param includeStampingDetails se includere anche i dettagli
   */
  public static void showTag(Person person, int year, int month, boolean includeStampingDetails, 
      boolean forAll, Long officeId) {

    if (person == null && !forAll) {
      flash.error("Selezionare una persona dall'elenco del personale.");
      listPersonForPrintTags(year, month, officeId);
    }
    Office office = officeDao.getOfficeById(officeId);
    rules.checkIfPermitted(office);
    List<PrintTagsInfo> list = Lists.newArrayList();
    if (!forAll) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month, false);

      List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
      historyStampingsList = printTagsManager.getHistoricalList(psDto, includeStampingDetails);
      PrintTagsInfo info = PrintTagsInfo.builder()
          .psDto(psDto)
          .person(person)
          .includeStampingDetails(includeStampingDetails)
          .historyStampingsList(historyStampingsList)
          .build();
      list.add(info);
      renderPDF(psDto, includeStampingDetails, historyStampingsList, list);
    } else {
      LocalDate date = new LocalDate(year, month, 1);
      List<Person> personList = personDao.list(
          Optional.<String>absent(),
          secureManager.officesReadAllowed(Security.getUser().get()),
          false, date, date.dayOfMonth().withMaximumValue(), true).list();
      
    }
    
  }

  /**
   * restituisce il template contenente la lista di persone attive per cui stampare il 
   * cartellino nell'anno e nel mese passati come parametro.
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void listPersonForPrintTags(int year, int month, Long officeId) {

    LocalDate date = new LocalDate(year, month, 1);
    Office office = officeDao.getOfficeById(officeId);
    List<Person> personList = personDao.list(
        Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        false, date, date.dayOfMonth().withMaximumValue(), true).list();
    boolean forAll = true;

    render(personList, date, year, month, forAll, office);
  }
}
