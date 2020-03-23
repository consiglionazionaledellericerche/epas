package controllers;

import static play.modules.pdf.PDF.renderPDF;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.OfficeDao;
import dao.PersonDao;
import dao.history.HistoryValue;
import dao.wrapper.IWrapperFactory;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.PrintTagsManager;
import manager.SecureManager;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Office;
import models.Person;
import models.Stamping;
import models.dto.OffSiteWorkingTemp;
import models.dto.PrintTagsInfo;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
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

    //conrtrollo chi ha chiamato questa funzionalità: se la persona per stampare il proprio 
    //cartellino o l'amministratore del personale.
    Office office = null;
    if (officeId == null) {
      //office = person.office;
      rules.checkIfPermitted(person);
    } else {
      office = officeDao.getOfficeById(officeId);
      rules.checkIfPermitted(office);
    }

    
    List<PrintTagsInfo> dtoList = Lists.newArrayList();
    List<Person> personList = Lists.newArrayList();

    LocalDate date = new LocalDate(year, month, 1);
    if (!forAll) {
      personList.add(person);
    } else {
      Set<Office> set = Sets.newHashSet();
      set.add(office);
      personList = personDao.list(
          Optional.<String>absent(),
          set,
          false, date, date.dayOfMonth().withMaximumValue(), true).list();
    }

    for (Person p : personList) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(p, year, month, false);
      log.debug("Creato il person stamping recap per {}", psDto.person.fullName());
      List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
      if (includeStampingDetails) {
        historyStampingsList = printTagsManager.getHistoricalList(psDto);
      }
      val stampingOwnersInDays = 
          printTagsManager.getStampingOwnerInDays(p, YearMonth.of(year, month));
      log.debug("Trovati {} utenti diversi che hanno inserito/timbrature nel mese {}/{} "
          + "per {}", 
          stampingOwnersInDays.keySet().size(), month, year, person.getFullname());

      List<OffSiteWorkingTemp> offSiteWorkingTemp = printTagsManager.getOffSiteStampings(psDto);
      PrintTagsInfo info = PrintTagsInfo.builder()
          .psDto(psDto)
          .person(p)
          .includeStampingDetails(includeStampingDetails)
          .offSiteWorkingTempList(offSiteWorkingTemp)
          .historyStampingsList(historyStampingsList)
          .stampingOwnersInDays(stampingOwnersInDays)
          .build();
      log.debug("Creato il PrintTagsInfo per {}", info.person.fullName());
      dtoList.add(info);
      log.debug("Inserito nella lista: {}", info.person.fullName());
    }
    renderPDF(dtoList);
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
    Set<Office> set = Sets.newHashSet();
    set.add(office);
    List<Person> personList = personDao.list(
        Optional.<String>absent(),
        set,
        false, date, date.dayOfMonth().withMaximumValue(), true).list();
    boolean forAll = true;

    render(personList, date, year, month, forAll, office);
  }
}
