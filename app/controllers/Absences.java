package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.QualificationDao;

import it.cnr.iit.epas.DateUtility;

import manager.AbsenceManager;
import manager.SecureManager;
import manager.YearlyAbsencesManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.YearlyAbsencesRecap;

import models.Office;
import models.Person;
import models.PersonDay;
import models.Qualification;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.QualificationMapping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

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
  private static SecureManager secureManager;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static YearlyAbsencesManager yearlyAbsencesManager;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static ConfigurationManager confManager;

  /**
   * Le assenze della persona nel mese.
   *
   * @param year  anno richiesto
   * @param month mese richiesto
   */
  public static void absences(final int year, final int month) {
    Person person = Security.getUser().get().person;
    YearMonth yearMonth = new YearMonth(year, month);
    Map<AbsenceType, Long> absenceTypeInMonth =
        absenceTypeDao.getAbsenceTypeInPeriod(person,
            DateUtility.getMonthFirstDay(yearMonth), Optional
                .fromNullable(DateUtility.getMonthLastDay(yearMonth)));

    render(absenceTypeInMonth, year, month);
  }

  /**
   * metodo che renderizza la pagina di visualizzazione delle assenze in un anno e in mese
   * specifico.
   *
   * @param absenceCode il codice di assenza
   * @param year        l'anno
   * @param month       il mese
   */
  public static void absenceInMonth(String absenceCode, int year, int month) {
    Person person = Security.getUser().get().person;
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

    rules.checkIfPermitted(absence.personDay.person.office);

    if (absenceFile != null) {
      absence.absenceFile = absenceFile;
      absence.save();

      flash.success("File allegato con successo.");
    }

    Stampings.personStamping(absence.personDay.person.id,
        absence.personDay.date.getYear(), absence.personDay.date.getMonthOfYear());

  }

  /**
   * metodo che permette di rimuovere un attachment da una assenza.
   *
   * @param absenceId l'id della assenza
   */
  public static void removeAttach(@Required Long absenceId) {

    Absence absence = absenceDao.getAbsenceById(absenceId);

    Verify.verify(absence.isPersistent(), "Assenza specificata inesistente!");

    absence.absenceFile.getFile().delete();

    absence.save();

    flash.success("File allegato rimosso con successo.");

    Stampings.personStamping(absence.personDay.person.id,
        absence.personDay.date.getYear(), absence.personDay.date.getMonthOfYear());

  }

  /**
   * Gli allegati alle assenze nel mese. Bisogna renderlo parametrico alla sede.
   *
   * @param year     anno
   * @param month    mese
   * @param officeId id office
   */
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
      if (absence.personDay.person.office.equals(office)) {
        absenceList.add(absence);
      }
    }
    List<AttachmentsPerCodeRecap> attachmentRecapList = new ArrayList<AttachmentsPerCodeRecap>();
    AttachmentsPerCodeRecap currentRecap = new AttachmentsPerCodeRecap();
    AbsenceType currentAbt = null;

    for (Absence abs : absenceList) {

      if (currentAbt == null) {

        currentAbt = abs.absenceType;
      } else if (!currentAbt.code.equals(abs.absenceType.code)) {

        //finalizza tipo
        /* evitato con la query abs.absenceFile is not null */
        if (currentRecap.absenceSameType.size() > 0) {
          attachmentRecapList.add(currentRecap);
        }
        currentRecap = new AttachmentsPerCodeRecap();
        //nuovo tipo
        currentAbt = abs.absenceType;
      }
      if (abs.absenceFile.get() != null) {

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
   * metodo che permette lo scaricamento di un determinato allegato in formato pdf.
   *
   * @param id l'id dell'allegato da scaricare
   */
  public static void downloadAttachment(long id) {

    Logger.debug("Assenza con id: %d", id);
    Absence absence = absenceDao.getAbsenceById(id);
    notFoundIfNull(absence);

    rules.checkIfPermitted(absence.personDay.person.office);

    response.setContentTypeIfNotSet(absence.absenceFile.type());
    Logger.debug("Allegato relativo all'assenza: %s", absence.absenceFile.getFile());
    renderBinary(absence.absenceFile.get(), absence.absenceFile.length());
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
    rules.checkIfPermitted(Security.getUser().get().person.office);
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

        FileInputStream fis = new FileInputStream(abs.absenceFile.getFile());

        zos.putNextEntry(new ZipEntry(abs.absenceFile.getFile().getName()));

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
  public static void manageAttachmentsPerPerson(Long personId, Integer year, Integer month) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    List<Absence> personAbsenceListWithFile = new ArrayList<Absence>();
    List<Absence> personAbsenceList = absenceDao
        .getAbsencesInPeriod(Optional.fromNullable(person), new LocalDate(year, month, 1),
            Optional.fromNullable(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()),
            false);

    for (Absence abs : personAbsenceList) {
      if (abs.absenceFile.get() != null) {
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

    rules.checkIfPermitted(person.office);

    //Se le date non sono specificate imposto il giorno corrente
    if (from == null || to == null) {
      from = LocalDate.now();
      to = LocalDate.now();
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

    rules.checkIfPermitted(person.office);

    List<Absence> absenceList = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person),
        Optional.<String>absent(), from, to,
        Optional.of(JustifiedTypeName.all_day), false, false);

    for (Absence abs : absenceList) {
      if (AbsenceTypeMapping.MISSIONE.is(abs.absenceType)) {
        missioni.add(abs);
      } else if (AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(abs.absenceType)
          || AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(abs.absenceType)
          || AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(abs.absenceType)) {
        ferie.add(abs);
      } else if (AbsenceTypeMapping.RIPOSO_COMPENSATIVO.is(abs.absenceType)) {
        riposiCompensativi.add(abs);
      } else {
        altreAssenze.add(abs);
      }
    }
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

    rules.checkIfPermitted(person.office);
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
    abt.code = "Totale";

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
   * @param year Render della pagina absencePerPerson.html che riassume le assenze annuali di una
   *             persona
   */

  public static void absencesPerPerson(Integer year) {

    //controllo sui parametri
    Optional<User> currentUser = Security.getUser();
    if (!currentUser.isPresent() || currentUser.get().person == null) {
      flash.error("Accesso negato.");
      renderTemplate("Application/indexAdmin.html");
    }
    User user = currentUser.get();
    //rendering
    if (year == null) {
      LocalDate now = new LocalDate();
      YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(
          user.person, now.getYear(), absenceDao.getYearlyAbsence(user.person, now.getYear()));
      render(yearlyAbsencesRecap);
    } else {
      YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(
          user.person, year.intValue(),
          absenceDao.getYearlyAbsence(user.person, year.intValue()));
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

    rules.checkIfPermitted(person.office);
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
    if (Security.getUser().get().person == null) {
      flash.error("Utente di sistema non abilitato alla funzionalità");
      Persons.list(null, null);
    }
    if (!(Boolean) confManager
        .configValue(Security.getUser().get().person.office, EpasParam.ABSENCES_FOR_EMPLOYEE)) {
      flash.error("Per accedere a questa funzione, occorre modificare il valore del "
          + "parametro 'Assenze visibili dai dipendenti'.");
      Stampings.stampings(year, month);
    }

    Person person = Security.getUser().get().person;
    List<Person> list = personDao.byOffice(person.office);
    LocalDate date = new LocalDate(year, month, day);
    List<PersonDay> pdList = personDayDao.getPersonDayForPeopleInDay(list, date);
    render(pdList, person, date);
  }

  public static class AttachmentsPerCodeRecap {

    List<Absence> absenceSameType = new ArrayList<Absence>();

    public String getCode() {
      return absenceSameType.get(0).absenceType.code;
    }

  }

}
