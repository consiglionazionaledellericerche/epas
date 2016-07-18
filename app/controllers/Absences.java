package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
import dao.UserDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import it.cnr.iit.epas.DateUtility;

import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.NotificationManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.SecureManager;
import manager.YearlyAbsencesManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.YearlyAbsencesRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Qualification;
import models.User;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.QualificationMapping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.JavaRules;
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
  private static JavaRules jRules;
  @Inject
  private static AbsenceTypeDao absenceTypeDao;
  @Inject
  private static QualificationDao qualificationDao;
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
  private static AbsenceHistoryDao absenceHistoryDao;
  @Inject
  private static YearlyAbsencesManager yearlyAbsencesManager;
  @Inject
  private static PersonManager personManager;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static ConfigurationManager confManager;
  @Inject
  private static UserDao userDao;
  @Inject
  private static NotificationManager notificationManager;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static PersonDayManager personDayManager;

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
        Optional.<JustifiedTimeAtWork>absent(),
        false,
        true);

    List<LocalDate> dateAbsences = FluentIterable.from(absences)
        .transform(AbsenceManager.AbsenceToDate.INSTANCE).toList();

    render(dateAbsences, absenceCode);
  }

  /**
   * metodo che renderizza la pagina di gestione dei codici di assenza.
   */
  public static void manageAbsenceCode() {

    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    render(absenceTypes);
  }

  /**
   * metodo che chiama la render della edit code per modificare/inserire un codice di assenza.
   */
  public static void insertCode() {

    render("@editCode");
  }

  /**
   * metodo che renderizza la pagina per gestire/inserire un codice di assenza.
   *
   * @param absenceCodeId l'id del codice di assenza che si intende modificare/inserire
   */
  public static void editCode(@Required Long absenceCodeId) {

    AbsenceType absenceType = absenceTypeDao.getAbsenceTypeById(absenceCodeId);
    AbsenceTypeGroup absenceTypeGroup = absenceType.absenceTypeGroup;

    boolean tecnologi = false;
    boolean tecnici = false;

    for (Qualification q : absenceType.qualifications) {
      tecnologi = !tecnologi ? QualificationMapping.TECNOLOGI.contains(q) : tecnologi;
      tecnici = !tecnici ? QualificationMapping.TECNICI.contains(q) : tecnici;
    }

    render(absenceType, absenceTypeGroup, tecnologi, tecnici);
  }

  /**
   * metodo che salva il nuovo/modificato codice di assenza.
   *
   * @param absenceType      il tipo di assenza
   * @param absenceTypeGroup il gruppo a cui fa riferimento un codice di assenza
   * @param tecnologi        se il codice di assenza è valido per i tecnologi
   * @param tecnici          se il codice di assenza è valido per i tecnici
   */
  public static void saveCode(@Valid AbsenceType absenceType,
      AbsenceTypeGroup absenceTypeGroup,
      boolean tecnologi, boolean tecnici) {

    //FIXME capire come mai senza il flash.clear si sovrappongono i messaggi
    flash.clear();
    if (validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@editCode", absenceType, absenceTypeGroup, tecnologi, tecnici);
    }
    Logger.info("tecnologi  %s - tecnici %s", tecnologi, tecnici);

    if (!(tecnologi || tecnici)) {
      flash.error("Selezionare almeno una categoria tra Tecnologi e Tecnici");
      render("@editCode", absenceType, absenceTypeGroup, tecnologi, tecnici);
    }

    absenceType.qualifications.clear();
    Range<Integer> qualifiche;
    if (tecnologi && tecnici) {
      qualifiche = QualificationMapping.TECNICI.getRange()
          .span(QualificationMapping.TECNOLOGI.getRange());
    } else if (tecnologi) {
      qualifiche = QualificationMapping.TECNOLOGI.getRange();
    } else {
      qualifiche = QualificationMapping.TECNICI.getRange();
    }

    for (int i = qualifiche.lowerEndpoint(); i <= qualifiche.upperEndpoint(); i++) {
      Qualification qual = qualificationDao.byQualification(i).orNull();
      absenceType.qualifications.add(qual);
    }

    if (!Strings.isNullOrEmpty(absenceTypeGroup.label) && !absenceType.isPersistent()) {
      absenceType.absenceTypeGroup = absenceTypeGroup;
      absenceTypeGroup.save();
    }

    absenceType.save();
    Logger.info("Inserito/modificato codice di assenza %s", absenceType.code);
    flash.success("Inserito/modificato codice di assenza %s", absenceType.code);

    manageAbsenceCode();
  }

  /**
   * metodo che renderizza la pagina di inserimento di una nuova assenza.
   *
   * @param personId l'id della persona di cui si vuole inserire l'assenza
   * @param dateFrom la data da cui si vuole inserire l'assenza
   */
  // FIXME sono stati rimossi i validatori perchè impedivano alle drools di funzionare correttamente
  // da ripristinare appena si risolve il problema .
  public static void blank(Long personId, LocalDate dateFrom, boolean forceInsert) {

    if (validation.hasErrors()) {
      flash.error(validation.errors().toString());
      render();
    }

    Person person = personDao.getPersonById(personId);
    Preconditions.checkNotNull(person);

    rules.checkIfPermitted(person);
    final LocalDate dateTo = dateFrom;

    render(person, dateFrom, dateTo, forceInsert);
  }

  /**
   * metodo che permette il salvataggio di un certo codice di assenza per una persona per un giorno
   * o un periodo temporale.
   *
   * @param personId    la persona per cui si vuole salvare l'assenza
   * @param dateFrom    la data da cui si vuole salvare l'assenza
   * @param dateTo      la data entro cui si vuole salvare l'assenza
   * @param absenceType il tipo di assenza da salvare
   * @param file        l'eventuale allegato
   */
  public static void save(Long personId, LocalDate dateFrom, LocalDate dateTo,
      @Valid AbsenceType absenceType, Blob file) {

    Person person = personDao.getPersonById(personId);
    if (person == null) {
      flash.error("La persona non esiste!");
      Persons.list(null, null);
    }
    if (Validation.hasErrors()) {

      response.status = 400;
      //flash.error(Web.msgHasErrors());

      render("@blank", person, dateFrom, dateTo);
    }
    final User user = Security.getUser().orNull();
    if (user != null && (user.person == null || userDao.hasAdminRoles(user))) {
      rules.checkIfPermitted(person);
    } else {
      // FIXME questa è una porcheria fatta perchè le drools in alcuni casi non funzionano come dovrebbero
      // da rimuovere non appena si riesce a risolvere il problema sulle drools
      jRules.checkForAbsences(absenceType, person);
    }

    AbsenceInsertReport air = absenceManager.insertAbsenceRecompute(
        person, dateFrom, Optional.fromNullable(dateTo),
        absenceType, Optional.fromNullable(file),
        Optional.<String>absent(), Optional.<Integer>absent());

    //Verifica errori generali nel periodo specificato
    if (air.hasWarningOrDaysInTrouble()) {
      flash.error(
          String.format("%s - %s", air.getWarnings().iterator().next(), air.getDatesInTrouble()));
    }

    //Verifica degli errori sui singoli giorni
    if (air.getTotalAbsenceInsert() == 0 && !air.getAbsences().isEmpty()) {

      Multimap<String, LocalDate> errors = ArrayListMultimap.create();

      for (AbsencesResponse ar : air.getAbsences()) {
        errors.put(ar.getWarning() + " [codice: " + ar.getAbsenceCode() + "]", ar.getDate());
      }
      flash.error(errors.toString());
    }

    //Verifica per eventuali giorni di reperibilità
    if (air.getAbsenceInReperibilityOrShift() > 0) {
      flash.error("Attenzione! verificare le reperibilità nei seguenti giorni : %s",
          air.datesInReperibilityOrShift());
    }

    //Messaggio di inserimento
    if (air.getTotalAbsenceInsert() > 0) {
      flash.success("Inserite %s assenze con codice %s",
          air.getTotalAbsenceInsert(),
          air.getAbsences().iterator().next().getAbsenceCode());
    }

    if (!Security.getUser().get().isSystemUser()) {
      if (Security.getUser().get().person.id.equals(person.id)
          && !personManager.isPersonnelAdmin(Security.getUser().get())) {
        air.getAbsences().forEach(ab -> {
          notificationManager.notifyAbsence(ab.getAbsenceAdded(), NotificationManager.CRUD.CREATE);
        });
        Stampings.stampings(dateFrom.getYear(), dateFrom.getMonthOfYear());
      }
    }

    Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
  }


  /**
   * metodo che renderizza la pagina di modifica di una determinata assenza.
   *
   * @param absence l'assenza da modificare
   */
  public static void edit(Absence absence) {

    notFoundIfNull(absence);

    final User user = Security.getUser().orNull();
    if (user != null && (user.person == null || userDao.hasAdminRoles(user))) {
      rules.checkIfPermitted(absence.personDay.person);
    } else {
      // FIXME questa è una porcheria fatta perchè le drools in alcuni casi non funzionano come dovrebbero
      // da rimuovere non appena si riesce a risolvere il problema sulle drools
      jRules.checkForAbsences(absence);
    }

    List<HistoryValue<Absence>> historyAbsence = absenceHistoryDao.absences(absence.id);

    LocalDate dateFrom = absence.personDay.date;
    LocalDate dateTo = absence.personDay.date;

    render(absence, dateFrom, dateTo, historyAbsence);
  }


  /**
   * metodo che cancella una certa assenza fino ad un certo periodo.
   *
   * @param absence l'assenza
   * @param dateTo  la data di fine periodo
   */
  public static void delete(@Required Absence absence, @Valid LocalDate dateTo) {

    Verify.verify(absence.isPersistent(), "Assenza specificata inesistente!");

    final User user = Security.getUser().orNull();
    if (user != null && (user.person == null || userDao.hasAdminRoles(user))) {
      rules.checkIfPermitted(absence.personDay.person);
    } else {
      // FIXME questa è una porcheria fatta perchè le drools in alcuni casi non funzionano come dovrebbero
      // da rimuovere non appena si riesce a risolvere il problema sulle drools
      jRules.checkForAbsences(absence);
    }

    Person person = absence.personDay.person;
    LocalDate dateFrom = absence.personDay.date;

    if (dateTo != null && dateTo.isBefore(dateFrom)) {
      flash.error("Errore nell'inserimento del campo Fino a, inserire una data valida. "
          + "Operazione annullata");
    }
    if (flash.contains("error")) {
      Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
    }

    //Logica
    int deleted = absenceManager
        .removeAbsencesInPeriod(person, dateFrom, dateTo, absence.absenceType);

    if (deleted > 0) {
      flash.success("Rimossi %s codici assenza di tipo %s", deleted, absence.absenceType.code);
    }

    if (!Security.getUser().get().isSystemUser()) {
      if (Security.getUser().get().person.id.equals(person.id)
          && !personManager.isPersonnelAdmin(Security.getUser().get())) {
        Stampings.stampings(dateFrom.getYear(), dateFrom.getMonthOfYear());
      }
    }

    Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
  }

  /**
   * metodo che permette l'attachment di un file a una assenza.
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
        Optional.<JustifiedTimeAtWork>absent(), true, false);
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
      } catch (IOException e) {

        e.printStackTrace();
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
        Optional.of(JustifiedTimeAtWork.AllDay), false, false);

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
          monthBegin, monthEnd, Optional.<JustifiedTimeAtWork>absent(), false, true);
    } else {
      absenceToRender = absenceDao.getAbsenceByCodeInPeriod(
          Optional.fromNullable(person), Optional.fromNullable(absenceTypeCode),
          monthBegin, monthEnd, Optional.<JustifiedTimeAtWork>absent(), false, true);
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

  public static void forceAbsences(int year, int month, Long personId) {

    if (personId == null) {
      personId = Long.valueOf(session.get("personSelected"));
    }
    if (year == 0) {
      year = Integer.parseInt(session.get("yearSelected"));
    }
    if (month == 0) {
      month = Integer.parseInt(session.get("monthSelected"));
    }
    Person person = personDao.getPersonById(personId);
    Preconditions.checkNotNull(person);

    rules.checkIfPermitted(person);

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    if (!wrPerson.isActiveInMonth(new YearMonth(year, month))) {

      flash.error("Non esiste situazione mensile per il mese di %s",
          person.fullName(), DateUtility.fromIntToStringMonth(month));

      YearMonth last = wrapperFactory.create(person).getLastActiveMonth();
      forceAbsences(last.getYear(), last.getMonthOfYear(), personId);
    }

    PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month, true);
    render(year, month, person, psDto);
  }

  public static void forceRemoval(Absence absence) {
    notFoundIfNull(absence);
    int year = absence.personDay.date.getYear();
    int month = absence.personDay.date.getMonthOfYear();
    if (absence.isPersistent()) {
      absenceManager.removeAbsencesInPeriod(absence.personDay.person,
          absence.personDay.date, absence.personDay.date, absence.absenceType);
      flash.success("Assenza Eliminata");
    }
    forceAbsences(year, month, null);
  }


  public static void forceInsert(Long personId, LocalDate dateFrom, LocalDate dateTo,
      @Valid AbsenceType absenceType) {

    Person person = personDao.getPersonById(personId);
    if (person == null) {
      flash.error("La persona non esiste!");
      Persons.list(null, null);
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      boolean forceInsert = true;
      render("@blank", person, dateFrom, dateTo, forceInsert);
    }

    rules.checkIfPermitted(person);

    Optional<AbsenceType> ferCode = absenceTypeDao
        .getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_FESTIVITA_SOPPRESSE_EPAS.getCode());

    if (ferCode.isPresent() && ferCode.get() == absenceType) {
      flash.error("Utilizzare i codici specifici d'assenza " +
          "per l'inserimento delle ferie (31, 32, 94)");
      int year = Integer.parseInt(session.get("yearSelected"));
      int month = Integer.parseInt(session.get("monthSelected"));
      forceAbsences(year, month, null);
    }

    LocalDate actualDate = dateFrom;
    while (!actualDate.isAfter(dateTo)) {
      final PersonDay personDay = personDayManager
          .getOrCreateAndPersistPersonDay(person, actualDate);
      Absence absence = new Absence();
      absence.absenceType = absenceType;
      absence.personDay = personDay;
      personDay.absences.add(absence);
      personDay.save();
      actualDate = actualDate.plusDays(1);
    }

    consistencyManager.updatePersonSituation(person.id, dateFrom);

    int year = Integer.parseInt(session.get("yearSelected"));
    int month = Integer.parseInt(session.get("monthSelected"));
    forceAbsences(year, month, null);
  }

  public static class AttachmentsPerCodeRecap {

    List<Absence> absenceSameType = new ArrayList<Absence>();

    public String getCode() {
      return absenceSameType.get(0).absenceType.code;
    }

  }

}
