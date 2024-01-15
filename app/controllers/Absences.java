/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import common.security.SecurityRules;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import helpers.ImageUtils;
import it.cnr.iit.epas.DateUtility;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.SecureManager;
import manager.YearlyAbsencesManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.YearlyAbsencesRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Role;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.AbsenceTypeMapping;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle assenze.
 */
@Slf4j
@With({Resecure.class})
public class Absences extends Controller {

  @Inject
  private static SecurityRules rules;
  @Inject
  private static AbsenceTypeDao absenceTypeDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  private static AbsenceManager absenceManager;
  @Inject
  private static SecureManager secureManager;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static YearlyAbsencesManager yearlyAbsencesManager;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static ConfigurationManager confManager;
  @Inject
  private static ConsistencyManager consistencyManager;

  /**
   * Le assenze della persona nel mese.
   *
   * @param year  anno richiesto
   * @param month mese richiesto
   */
  public static void absences(final int year, final int month) {
    Person person = Security.getUser().get().getPerson();
    YearMonth yearMonth = new YearMonth(year, month);
    Map<AbsenceType, Long> absenceTypeInMonth =
        absenceTypeDao.getAbsenceTypeInPeriod(person,
            DateUtility.getMonthFirstDay(yearMonth), Optional
                .fromNullable(DateUtility.getMonthLastDay(yearMonth)));

    render(absenceTypeInMonth, year, month);
  }

  /**
   * Metodo che renderizza la pagina di visualizzazione delle assenze in un anno e in mese
   * specifico.
   *
   * @param absenceCode il codice di assenza
   * @param year        l'anno
   * @param month       il mese
   */
  public static void absenceInMonth(String absenceCode, int year, int month) {
    Person person = Security.getUser().get().getPerson();
    YearMonth yearMonth = new YearMonth(year, month);

    List<Absence> absences = absenceDao.getAbsenceByCodeInPeriod(
        Optional.fromNullable(person),
        Optional.fromNullable(absenceCode),
        DateUtility.getMonthFirstDay(yearMonth),
        DateUtility.getMonthLastDay(yearMonth),
        Optional.<JustifiedTypeName>absent(),
        false,
        true);

    List<LocalDate> dateAbsences = FluentIterable.from(absences)
        .transform(AbsenceManager.AbsenceToDate.INSTANCE).toList();

    render(dateAbsences, absenceCode);
  }

  /**
   * Metodo che permette l'attachment di un file a una assenza.
   *
   * @param absence     l'assenza
   * @param absenceFile il file associato a quella assenza
   */
  public static void addAttach(@Required Absence absence, Blob absenceFile) {

    Verify.verify(absence.isPersistent(), "Assenza specificata inesistente!");
    
    Optional<User> currentUser = Security.getUser();
    if (!currentUser.isPresent() || currentUser.get().hasRoles(Role.PERSONNEL_ADMIN)) {
      rules.checkIfPermitted(absence.getPersonDay().getPerson().getOffice());
    } else {
      rules.checkIfPermitted(absence);
    }    

    if (absenceFile != null) {
      absence.setAbsenceFile(absenceFile);
      absence.save();

      flash.success("File allegato con successo.");
    }
    
    if (!currentUser.isPresent() || currentUser.get().hasRoles(Role.PERSONNEL_ADMIN)) {
      Stampings.personStamping(absence.getPersonDay().getPerson().id,
          absence.getPersonDay().getDate().getYear(), 
          absence.getPersonDay().getDate().getMonthOfYear());
    } else {
      Stampings.stampings(absence.getPersonDay().getDate().getYear(), 
          absence.getPersonDay().getDate().getMonthOfYear());
    }    

  }
  
  /**
   * Aggiunge al tempo a lavoro il quantitativo di ore e minuti inserito per quel giorno.
   *
   * @param absenceId l'identificativo dell'assenza
   * @param hours le ore da salvare
   * @param minutes i minuti da salvare
   */
  public static void overtimeAbsence(Long absenceId, Integer hours, Integer minutes) {
    //TODO: va completata la business logic di questo metodo!!!
    Absence absence = absenceDao.getAbsenceById(absenceId);
    Verify.verify(absence.isPersistent(), "Assenza specificata inesistente!");
    rules.checkIfPermitted(absence.getPersonDay().getPerson().getOffice());
    if (hours == null || minutes == null) {
      flash.error("Quantità non corrette di ore e minuti.");
      Stampings.personStamping(absence.getPersonDay().getPerson().id,
          absence.getPersonDay().getDate().getYear(), 
          absence.getPersonDay().getDate().getMonthOfYear());
    }

    
    int overtimeMission = hours * 60 + minutes;
    absence.getPersonDay().setWorkingTimeInMission(overtimeMission);
    absence.getPersonDay().save();
    consistencyManager.updatePersonSituation(absence.getPersonDay().getPerson().id, 
        absence.getPersonDay().getDate());
    flash.success("Ore in più per l'assenza salvate correttamente");
    Stampings.personStamping(absence.getPersonDay().getPerson().id,
        absence.getPersonDay().getDate().getYear(), 
        absence.getPersonDay().getDate().getMonthOfYear());
  }

  /**
   * metodo che permette di rimuovere un attachment da una assenza.
   *
   * @param absenceId l'id della assenza
   */
  public static void removeAttach(@Required Long absenceId) {

    Absence absence = absenceDao.getAbsenceById(absenceId);

    Verify.verify(absence.isPersistent(), "Assenza specificata inesistente!");

    absence.getAbsenceFile().getFile().delete();

    absence.save();

    flash.success("File allegato rimosso con successo.");

    Optional<User> currentUser = Security.getUser();
    if (!currentUser.isPresent() || currentUser.get().hasRoles(Role.PERSONNEL_ADMIN)) {
      Stampings.personStamping(absence.getPersonDay().getPerson().id,
          absence.getPersonDay().getDate().getYear(), 
          absence.getPersonDay().getDate().getMonthOfYear());
    } else {
      Stampings.stampings(absence.getPersonDay().getDate().getYear(), 
          absence.getPersonDay().getDate().getMonthOfYear());
    }
    

  }

  /**
   * Gli allegati alle assenze nel mese. Bisogna renderlo parametrico alla sede.
   *
   * @param year     anno
   * @param month    mese
   * @param officeId id office
   */
  @SuppressWarnings("resource")
  public static void manageAttachmentsPerCode(Integer year, Integer month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    LocalDate beginMonth = new LocalDate(year, month, 1);

    //Prendere le assenze ordinate per tipo
    List<Absence> absenceListAux = absenceDao.getAbsencesInPeriod(
        Optional.<Person>absent(), beginMonth,
        Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()), true);

    //Provvisoriamente mangengo solo quelle di persone dell'office
    List<Absence> absenceList = Lists.newArrayList();
    for (Absence absence : absenceListAux) {
      if (absence.getPersonDay().getPerson().getOffice().equals(office)) {
        absenceList.add(absence);
      }
    }
    List<AttachmentsPerCodeRecap> attachmentRecapList = new ArrayList<AttachmentsPerCodeRecap>();
    AttachmentsPerCodeRecap currentRecap = new AttachmentsPerCodeRecap();
    AbsenceType currentAbt = null;

    for (Absence abs : absenceList) {

      if (currentAbt == null) {

        currentAbt = abs.getAbsenceType();
      } else if (!currentAbt.getCode().equals(abs.getAbsenceType().getCode())) {

        //finalizza tipo
        /* evitato con la query abs.absenceFile is not null */
        if (currentRecap.absenceSameType.size() > 0) {
          attachmentRecapList.add(currentRecap);
        }
        currentRecap = new AttachmentsPerCodeRecap();
        //nuovo tipo
        currentAbt = abs.getAbsenceType();
      }
      if (abs.getAbsenceFile() != null && abs.getAbsenceFile().get() != null) {
        currentRecap.absenceSameType.add(abs);
      }
    }

    //finalizza ultimo tipo
    if (currentRecap.absenceSameType.size() > 0) {
      attachmentRecapList.add(currentRecap);
    }

    render(attachmentRecapList, year, month);
  }

  /**
   * Metodo che permette lo scaricamento di un determinato allegato in formato pdf.
   *
   * @param id l'id dell'allegato da scaricare
   */
  @SuppressWarnings("resource")
  public static void downloadAttachment(long id) {

    log.debug("Assenza con id: {}", id);
    Absence absence = absenceDao.getAbsenceById(id);
    notFoundIfNull(absence);

    Optional<User> currentUser = Security.getUser();
    if (!currentUser.isPresent() || currentUser.get().hasRoles(Role.PERSONNEL_ADMIN)) {
      rules.checkIfPermitted(absence.getPersonDay().getPerson().getOffice());
    } else {
      rules.checkIfPermitted(absence);
    }

    response.setContentTypeIfNotSet(absence.getAbsenceFile().type());
    String filename = String.format("assenza-%s-%s",
        absence.getPersonDay().getPerson().getFullname().replace(" ", "-"), 
        absence.getAbsenceDate());
    if (ImageUtils.fileExtension(absence.getAbsenceFile()).isPresent()) {
      filename = String.format("%s%s", filename, 
          ImageUtils.fileExtension(absence.getAbsenceFile()).get());
    }

    log.debug("Allegato relativo all'assenza: {}", absence.getAbsenceFile().getFile());
    renderBinary(absence.getAbsenceFile().get(), filename, absence.getAbsenceFile().length());
  }

  /**
   * metodo che ritorna un file .zip contenente tutti gli allegati di un certo mese/anno.
   *
   * @param code  il codice di assenza
   * @param year  l'anno
   * @param month il mese
   * @throws IOException eccezione di IO
   */
  public static void zipAttachment(String code, Integer year, Integer month) throws IOException {
    rules.checkIfPermitted(Security.getUser().get().getPerson().getOffice());
    FileOutputStream fos = new FileOutputStream("attachment" + '-' + code + ".zip");
    ZipOutputStream zos = new ZipOutputStream(fos);


    List<Absence> absList = absenceDao.getAbsenceByCodeInPeriod(
        Optional.<Person>absent(), Optional.fromNullable(code),
        new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
        Optional.<JustifiedTypeName>absent(), true, false);
    byte[] buffer = new byte[1024];

    for (Absence abs : absList) {
      try {

        FileInputStream fis = new FileInputStream(abs.getAbsenceFile().getFile());

        zos.putNextEntry(new ZipEntry(abs.getAbsenceFile().getFile().getName()));

        int length;
        while ((length = fis.read(buffer)) >= 0) {
          zos.write(buffer, 0, length);
        }

        zos.closeEntry();

        fis.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    zos.close();

    renderBinary(new File("attachment" + '-' + code + ".zip"));
  }

  /**
   * Gli allegati del dipendente nel mese selezionato.
   *
   * @param personId dipendente
   * @param year     anno
   * @param month    mese
   */
  @SuppressWarnings("resource")
  public static void manageAttachmentsPerPerson(Long personId, Integer year, Integer month) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.getOffice());

    List<Absence> personAbsenceListWithFile = new ArrayList<Absence>();
    List<Absence> personAbsenceList = absenceDao
        .getAbsencesInPeriod(Optional.fromNullable(person), new LocalDate(year, month, 1),
            Optional.fromNullable(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()),
            false);

    for (Absence abs : personAbsenceList) {
      if (abs.getAbsenceFile() != null && abs.getAbsenceFile().get() != null) {
        personAbsenceListWithFile.add(abs);
      }
    }
    render(personAbsenceListWithFile, year, month, person);
  }

  /**
   * Vista delle assenze nel periodo.
   *
   * @param personId dipendente
   * @param from     da
   * @param to       a
   */
  public static void absenceInPeriod(Long personId, LocalDate from, LocalDate to) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.getOffice());

    //Se le date non sono specificate imposto il giorno corrente
    if (from == null || to == null) {
      from = LocalDate.now();
      to = LocalDate.now();
    }

    class DateComparator implements Comparator<Absence> {
      @Override
      public int compare(Absence a, Absence b) {
        return a.getPersonDay().getDate().compareTo(b.getPersonDay().getDate());
      }
    }
    
    List<Absence> missioni = Lists.newArrayList();
    List<Absence> ferie = Lists.newArrayList();
    List<Absence> riposiCompensativi = Lists.newArrayList();
    List<Absence> altreAssenze = Lists.newArrayList();

    List<Person> personList = personDao.list(Optional.<String>absent(),
        secureManager.officesReadAllowed(Security.getUser().get()),
        false, from, to, true).list();

    if (from.isAfter(to)) {
      flash.error("Intervallo non valido (%s - %s)", from, to);
      render(personList, person, from, to, missioni, ferie, riposiCompensativi, altreAssenze);
    }

    rules.checkIfPermitted(person.getOffice());

    List<Absence> absenceList = absenceDao.getAbsencesInPeriod(Optional.fromNullable(person), 
        from, Optional.fromNullable(to), false);

    for (Absence abs : absenceList) {
      if (AbsenceTypeMapping.MISSIONE.is(abs.getAbsenceType())) {
        missioni.add(abs);
      } else if (AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(abs.getAbsenceType())
          || AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(abs.getAbsenceType())
          || AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(abs.getAbsenceType())) {
        ferie.add(abs);
      } else if (AbsenceTypeMapping.RIPOSO_COMPENSATIVO.is(abs.getAbsenceType())) {
        riposiCompensativi.add(abs);
      } else {
        altreAssenze.add(abs);
      }
    }

    Collections.sort(absenceList, new DateComparator());
    render(personList, person, absenceList, from, to, missioni, ferie,
        riposiCompensativi, altreAssenze, person.id);

  }

  /**
   * Le assenze effettuate nell'anno dalla persona.
   *
   * @param personId persona
   * @param year     anno
   */
  public static void yearlyAbsences(Long personId, int year) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.getOffice());
    YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(
        person, year, absenceDao.getYearlyAbsence(person, year));
    render(yearlyAbsencesRecap, year, personId, person);

  }

  /**
   * Le assenze effettuate nel mese dei dipendenti della sede.
   *
   * @param year     year
   * @param month    month
   * @param officeId sede
   */
  public static void showGeneralMonthlyAbsences(int year, int month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    Table<Person, AbsenceType, Integer> tableMonthlyAbsences = TreeBasedTable
        .create(yearlyAbsencesManager.personNameComparator,
            yearlyAbsencesManager.absenceCodeComparator);
    AbsenceType abt = new AbsenceType();
    abt.setCode("Totale");

    List<Person> persons = personDao.list(
        Optional.<String>absent(),
        Sets.newHashSet(office),
        false, new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true).list();

    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
    tableMonthlyAbsences = yearlyAbsencesManager
        .populateMonthlyAbsencesTable(persons, abt, begin, end);
    int numberOfDifferentAbsenceType = tableMonthlyAbsences.columnKeySet().size();

    render(tableMonthlyAbsences, year, month, office, numberOfDifferentAbsenceType);

  }

  /**
   * Metodo che ritorna le assenze nell'anno per una persona.
   *
   * @param year l'anno di riferimento
   */
  public static void absencesPerPerson(Integer year) {

    //controllo sui parametri
    Optional<User> currentUser = Security.getUser();
    if (!currentUser.isPresent() || currentUser.get().getPerson() == null) {
      flash.error("Accesso negato.");
      renderTemplate("Application/indexAdmin.html");
    }
    User user = currentUser.get();
    //rendering
    if (year == null) {
      LocalDate now = new LocalDate();
      YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(
          user.getPerson(), now.getYear(), 
          absenceDao.getYearlyAbsence(user.getPerson(), now.getYear()));
      render(yearlyAbsencesRecap);
    } else {
      YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(
          user.getPerson(), year.intValue(),
          absenceDao.getYearlyAbsence(user.getPerson(), year.intValue()));
      render(yearlyAbsencesRecap);
    }
  }

  /**
   * metodo che renderizza la pagina di visualizzazione delle assenze mensili di una persona.
   *
   * @param personId        id della persona
   * @param year            l'anno
   * @param month           il mese
   * @param absenceTypeCode il codice di assenza
   * @throws InstantiationException eventuale eccezione di instanziazione gestita
   * @throws IllegalAccessException eventuale eccezione di accesso illegale gestita
   */
  public static void showPersonMonthlyAbsences(Long personId, Integer year,
      Integer month, String absenceTypeCode) throws InstantiationException, IllegalAccessException {

    LocalDate monthBegin = new LocalDate(year, month, 1);
    LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.getOffice());
    List<Absence> absenceToRender = new ArrayList<Absence>();

    if (absenceTypeCode.equals("Totale")) {
      absenceToRender = absenceDao.getAbsenceByCodeInPeriod(
          Optional.fromNullable(person), Optional.<String>absent(),
          monthBegin, monthEnd, Optional.<JustifiedTypeName>absent(), false, true);
    } else {
      absenceToRender = absenceDao.getAbsenceByCodeInPeriod(
          Optional.fromNullable(person), Optional.fromNullable(absenceTypeCode),
          monthBegin, monthEnd, Optional.<JustifiedTypeName>absent(), false, true);
    }

    render(person, absenceToRender);
  }

  /**
   * metodo che renderizza la lista dei dipendenti per verificare se siano o meno a lavoro un certo
   * giorno.
   *
   * @param year  l'anno
   * @param month il mese
   * @param day   il giorno
   */
  public static void absencesVisibleForEmployee(int year, int month, int day) {
    if (Security.getUser().get().getPerson() == null) {
      flash.error("Utente di sistema non abilitato alla funzionalità");
      Persons.list(null, null);
    }
    if (!(Boolean) confManager
        .configValue(Security.getUser().get().getPerson().getOffice(), 
            EpasParam.ABSENCES_FOR_EMPLOYEE)) {
      flash.error("Per accedere a questa funzione, occorre modificare il valore del "
          + "parametro 'Assenze visibili dai dipendenti'.");
      Stampings.stampings(year, month);
    }
    LocalDate date;
    if (year == 0 || month == 0 || day == 0) {
      date = LocalDate.now();
      flash.error("Non sono stati indicati correttamente anno, mese e giorno per la richiesta. "
          + "Mostrate le assenze di oggi.");
    } else {
      date = new LocalDate(year, month, day);
    }
    Person person = Security.getUser().get().getPerson();
    List<Person> list = personDao.byOffice(person.getOffice());
    List<PersonDay> pdList = personDayDao.getPersonDayForPeopleInDay(list, date);
    render(pdList, person, date);
  }

  /**
   * DTO per contenere le assenze dello stesso tipo.
   */
  public static class AttachmentsPerCodeRecap {

    List<Absence> absenceSameType = new ArrayList<Absence>();

    public String getCode() {
      return absenceSameType.get(0).getAbsenceType().getCode();
    }

  }

  public static void ical(Optional<String> absenceCode, LocalDate begin, LocalDate end) {
    if (Security.getUser().get().getPerson() == null) {
      log.warn("Utente di sistema non abilitato alla funzionalità Absences::ical");
      badRequest("Utente di sistema non abilitato alla funzionalità Absences::ical");
    }
    if (begin == null || end == null || begin.isAfter(end)) {
      badRequest("Parametri begin e end obbligatori o non corretti");
    }
    if (begin.isBefore(end.minusYears(1))) {
      badRequest("Periodo troppo lungo, il periodo massimo è di 1 anno");
    }
    val absences = absenceDao.getAbsenceByCodeInPeriod(
        Optional.of(Security.getUser().get().getPerson()), absenceCode, begin, end, Optional.absent(),
            false, true);
    try {
      Calendar calendar =
          absenceManager.createIcsAbsencesCalendar(begin, end, absences);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      CalendarOutputter outputter = new CalendarOutputter();
      outputter.output(calendar, bos);

      response.setHeader("Content-Type", "application/ics");
      InputStream is = new ByteArrayInputStream(bos.toByteArray());
      renderBinary(is, "assenze.ics");
      bos.close();
      is.close();
    } catch (IOException ex) {
      log.error("Io exception building ical", ex);
      error("Io exception building ical");
    }
  }

}