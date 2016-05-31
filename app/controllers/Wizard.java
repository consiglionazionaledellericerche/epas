package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import dao.QualificationDao;
import dao.RoleDao;
import dao.WorkingTimeTypeDao;

import helpers.validators.StringIsTime;

import lombok.extern.slf4j.Slf4j;

import manager.ContractManager;
import manager.OfficeManager;
import manager.UserManager;

import models.Contract;
import models.Institute;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.data.validation.CheckWith;
import play.data.validation.Email;
import play.data.validation.Equals;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.libs.Codec;
import play.mvc.Controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;


/**
 * Wizard per la configurazione iniziale di ePAS.
 * TODO: il wizard non è più mantenuto nella parte di salvataggio della configurazione iniziale.
 *  Se si vuole riabilitare utilizzare la nuova gestione della configurazione. 
 * @author daniele
 */
@Slf4j
@Deprecated
public class Wizard extends Controller {

  public static final String STEPS_KEY = "steps";
  public static final String PROPERTIES_KEY = "properties";
  public static final int LAST_STEP = 4;
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
  @Inject
  private static OfficeManager officeManager;
  @Inject
  private static QualificationDao qualificationDao;
  @Inject
  private static WorkingTimeTypeDao workingTimeTypeDao;
  @Inject
  private static RoleDao roleDao;
  @Inject
  private static ContractManager contractManager;

  @Inject
  private static UserManager userManager;

  public static List<WizardStep> createSteps() {
    return ImmutableList
        .of(WizardStep.of("Cambio password Admin", "changeAdminPsw", 0),
            WizardStep.of("Nuovo ufficio", "setOffice", 1),
            WizardStep.of("Configurazione generale", "setGenConf", 2),
            WizardStep.of("Creazione Ruolo per l'amministrazione", "seatManagerRole", 3),
            WizardStep.of("Riepilogo", "summary", 4));
  }

  public static void wizard(int step) {
    Preconditions.checkNotNull(step);

    //  Recupero dalla cache
    Long officeCount = Cache.get(Resecure.OFFICE_COUNT, Long.class);

    if (officeCount == null) {
      officeCount = Office.count();
      Cache.add(Resecure.OFFICE_COUNT, officeCount);
    }

    if (officeCount > 0) {
      flash.error("Impossibile accedere alla procedura di Wizard se è già presente un Ufficio"
          + " nel sistema");
      Institutes.list(null);
    }

    double percent = 0;

    //  Recupero dalla cache
    List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);

    if (steps == null) {
      steps = createSteps();
      Cache.safeAdd(STEPS_KEY, steps, "10mn");
    }

    //  Recupero dalla cache
    Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);

    if (properties == null) {
      try {
        properties = new Properties();
        properties.load(new FileInputStream("conf/Wizard_Properties.conf"));
      } catch (IOException f) {
        log.error("Impossibile caricare il file Wizard_Properties.conf per la procedura di Wizard");
      }
      Cache.safeAdd(PROPERTIES_KEY, properties, "10mn");
    }

    int stepsCompleted = Collections2.filter(steps, new Predicate<WizardStep>() {
        @Override
        public boolean apply(WizardStep step) {
          return step.completed;
        }
      }).size();

    percent = stepsCompleted * (100 / steps.size());

    if (step < 0 | step > steps.size() | (step != 0 && !steps.get(step - 1).completed)) {
      step = stepsCompleted;
    }

    if (step > 0) {
      try {
        properties.store(new FileOutputStream("conf/Wizard_Properties.conf"), "Wizard values file");
        Logger.info("Salvato file Wizard_Properties.conf");
      } catch (IOException e) {
        flash.error(e.getMessage());
      }
    }

    // Submit
    if (stepsCompleted == steps.size()) {
      Cache.clear();
      submit();
    }

    WizardStep currentStep = steps.get(step);

    if (currentStep.index == LAST_STEP) {

      final String name = properties.getProperty("personnelAdminName");
      final String surname = properties.getProperty("personnelAdminSurname");

      final String managerUser = userManager.generateUserName(name, surname);

      render("@" + currentStep.template, steps, currentStep, percent, properties, managerUser);
    }

    render("@" + currentStep.template, steps, currentStep, percent, properties);
  }

  /**
   * Step 1 del Wizard, cambio password Admin.
   *
   */
  public static void changeAdminPsw(
      int stepIndex, @Required String adminPassword,
      @Required @Equals(value = "adminPassword", message = "Le password non corrispondono")
      String adminPasswordRetype) {

    if (validation.hasErrors()) {
      params.flash();
      validation.keep();
      wizard(stepIndex);
    }

    List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);

    if (steps != null) {
      properties.setProperty("adminPassword", adminPassword);

      if (!steps.get(stepIndex).completed) {
        steps.get(stepIndex).complete();
        Logger.info("Completato lo step %s del wizard", stepIndex);
      }

      Cache.safeSet(STEPS_KEY, steps, "10mn");
      Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    }

    wizard(stepIndex + 1);
  }

  /**
   * STEP 2 Creazione Istituto e Ufficio.
   */
  public static void setOffice(int stepIndex, @Valid Institute institute, @Valid Office office) {

    if (validation.hasErrors()) {
      params.flash();
      validation.keep();
      wizard(stepIndex);
    }

    List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);

    if (steps != null) {

      properties.setProperty("instituteName", institute.name);
      properties.setProperty("instituteCds", institute.cds);
      properties.setProperty("instituteCode", institute.code);
      properties.setProperty("officeName", office.name);
      properties.setProperty("officeCode", office.code);
      properties.setProperty("officeCodeId", office.codeId);
      properties.setProperty("officeAddress", office.address);

      if (office.joiningDate != null) {
        properties.setProperty("officeJoiningDate", office.joiningDate.toString(dtf));
      }

      if (!steps.get(stepIndex).completed) {
        steps.get(stepIndex).complete();
      }

      steps.get(stepIndex).complete();
      Cache.safeSet(STEPS_KEY, steps, "10mn");
      Cache.safeSet(PROPERTIES_KEY, properties, "10mn");

    }
    wizard(stepIndex + 1);
  }

  /**
   * STEP 3 Configurazione Generale.
   */
  public static void setGenConf(int stepIndex, @Required String dateOfPatron,
      @Required @CheckWith(StringIsTime.class) String lunchPauseStart,
      @Required @CheckWith(StringIsTime.class) String lunchPauseEnd,
      @Email String emailToContact) {

    if (validation.hasErrors()) {
      params.flash();
      validation.keep();
      wizard(stepIndex);
    }

    List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);

    if (steps != null) {

      properties.setProperty("dateOfPatron", dateOfPatron);
      properties.setProperty("lunchPauseStart", lunchPauseStart);
      properties.setProperty("lunchPauseEnd", lunchPauseEnd);
      properties.setProperty("emailToContact", emailToContact);

      if (!steps.get(stepIndex).completed) {
        steps.get(stepIndex).complete();
        Logger.info("Completato lo step %s del wizard", stepIndex);
      }

      Cache.safeSet(STEPS_KEY, steps, "10mn");
      Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    }
    wizard(stepIndex + 1);
  }

  /**
   * STEP 4 Creazione Profilo per l'amministratore.
   */
  public static void seatManagerRole(int stepIndex, Person person, @Required int qualification,
      @Required LocalDate beginDate, LocalDate endContract,
      @Required String managerPassword,
      @Required @Equals(value = "managerPassword",  message = "Le password non corrispondono")
      String managerPasswordRetype) {

    validation.required(person.name);
    validation.required(person.surname);
    validation.required(person.email);

    if (validation.hasErrors()) {
      params.flash();
      validation.keep();
      wizard(stepIndex);
    }

    List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);

    if (steps != null) {

      properties.setProperty("personnelAdminName", person.name);
      properties.setProperty("personnelAdminSurname", person.surname);
      properties.setProperty("personnelAdminEmail", person.email);
      properties.setProperty("personnelAdminQualification", Integer.toString(qualification));
      properties.setProperty("personnelAdminPassword", managerPassword);
      properties.setProperty("personnelAdminbeginDate", beginDate.toString(dtf));

      if (person.number != null) {
        properties.setProperty("personnelAdminNumber", person.number.toString());
      }
      if (person.birthday != null) {
        properties.setProperty("personnelAdminBirthday", person.birthday.toString(dtf));
      }
      if (endContract != null) {
        properties.setProperty("personnelAdminEndContract", endContract.toString(dtf));
      }

      if (!steps.get(stepIndex).completed) {
        steps.get(stepIndex).complete();
        Logger.info("Completato lo step %s del wizard", stepIndex);
      }

      Cache.safeSet(STEPS_KEY, steps, "10mn");
      Cache.safeSet(PROPERTIES_KEY, properties, "10mn");
    }

    wizard(stepIndex + 1);
  }

  /**
   * STEP 2 Creazione Istituto e Ufficio.
   */
  public static void summary(int stepIndex) {

    List<WizardStep> steps = Cache.get(STEPS_KEY, List.class);
    Properties properties = Cache.get(PROPERTIES_KEY, Properties.class);

    if (steps != null) {

      if (!steps.get(stepIndex).completed) {
        steps.get(stepIndex).complete();
      }

      steps.get(stepIndex).complete();
      Cache.safeSet(STEPS_KEY, steps, "10mn");
      Cache.safeSet(PROPERTIES_KEY, properties, "10mn");

    }
    wizard(stepIndex + 1);
  }

  private static void submit() {

    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream("conf/Wizard_Properties.conf"));
    } catch (IOException f) {
      Logger.error("Impossibile caricare il file Wizard_Properties.conf durante il Wizard");
    }

    //  Creazione admin
    User adminUser = new User();
    adminUser.username = Role.ADMIN;
    adminUser.password = Codec.hexMD5(properties.getProperty("adminPassword"));
    adminUser.save();

    //  Creazione Istituto e Sede

    //  Istituto
    final Institute institute = new Institute();
    institute.name = properties.getProperty("instituteName");
    institute.cds = properties.getProperty("instituteCds");
    institute.code = properties.getProperty("instituteCode");
    institute.save();

    //  Sede
    final Office office = new Office();
    office.name = properties.getProperty("officeName");
    office.code = properties.getProperty("officeCode");
    office.codeId = properties.getProperty("officeCodeId");
    office.address = properties.getProperty("officeAddress");

    if (properties.containsKey("officeJoiningDate")) {
      office.joiningDate = LocalDate.parse(properties.getProperty("officeJoiningDate"), dtf);
    }

    office.institute = institute;
    office.save();

    //  Invalido la cache sul conteggio degli uffici
    Cache.safeDelete(Resecure.OFFICE_COUNT);

//    List<String> lunchStart = Splitter.on(":").trimResults()
//        .splitToList(properties.getProperty("lunchPauseStart"));
//
//    List<String> lunchStop = Splitter.on(":").trimResults()
//        .splitToList(properties.getProperty("lunchPauseEnd"));
//
//    List<String> dateOfPatron = Splitter.on("/").trimResults()
//        .splitToList(properties.getProperty("dateOfPatron"));
//
//    confGeneralManager.saveConfGeneral(Parameter.INIT_USE_PROGRAM, office,
//        Optional.fromNullable(LocalDate.now().toString()));
//
//    confGeneralManager.saveConfGeneral(Parameter.DAY_OF_PATRON, office,
//        Optional.fromNullable(dateOfPatron.get(0)));
//
//    confGeneralManager.saveConfGeneral(Parameter.MONTH_OF_PATRON, office,
//        Optional.fromNullable(dateOfPatron.get(1)));
//
//    confGeneralManager.saveConfGeneral(Parameter.MEAL_TIME_START_HOUR, office,
//        Optional.fromNullable(lunchStart.get(0)));
//
//    confGeneralManager.saveConfGeneral(Parameter.MEAL_TIME_START_MINUTE, office,
//        Optional.fromNullable(lunchStart.get(1)));
//
//    confGeneralManager.saveConfGeneral(Parameter.MEAL_TIME_END_HOUR, office,
//        Optional.fromNullable(lunchStop.get(0)));
//
//    confGeneralManager.saveConfGeneral(Parameter.MEAL_TIME_END_MINUTE, office,
//        Optional.fromNullable(lunchStop.get(1)));
//
//    confGeneralManager.saveConfGeneral(Parameter.EMAIL_TO_CONTACT, office,
//        Optional.fromNullable(properties.getProperty("emailToContact")));

    officeManager.setSystemUserPermission(office);

    //Creazione persona Amministratore

    Person person = new Person();
    person.name = properties.getProperty("personnelAdminName");
    person.surname = properties.getProperty("personnelAdminSurname");
    person.email = properties.getProperty("personnelAdminEmail");

    person.qualification = qualificationDao.getQualification(Optional
        .fromNullable(Integer.parseInt(properties.getProperty("personnelAdminQualification"))),
        Optional.<Long>absent(), false).get(0);

    if (properties.containsKey("personnelAdminNumber")) {
      person.number = Integer.parseInt(properties.getProperty("personnelAdminNumber"));
    }
    if (properties.containsKey("personnelAdminBirthday")) {
      person.birthday = LocalDate.parse(properties.getProperty("personnelAdminBirthday"), dtf);
    }

    person.office = office;
    person.save();

    //    creazione contratto
    Contract contract = new Contract();

    LocalDate contractBegin = LocalDate
        .parse(properties.getProperty("personnelAdminbeginDate"), dtf);

    LocalDate contractEnd = null;

    if (properties.containsKey("personnelAdminEndContract")) {
      contractEnd = LocalDate.parse(properties.getProperty("personnelAdminEndContract"), dtf);
    }
    contract.beginDate = contractBegin;
    contract.endDate = contractEnd;

    contract.onCertificate = true;
    contract.person = person;

    contract.save();

    WorkingTimeType wtt = workingTimeTypeDao
        .workingTypeTypeByDescription("Normale", Optional.<Office>absent());

    contractManager.properContractCreate(contract, wtt, true);

    User manager = userManager.createUser(person);
    manager.password = Codec.hexMD5(properties.getProperty("personnelAdminPassword"));
    manager.save();

    //  Assegnamento dei permessi all'utente creato
    officeManager.setSystemUserPermission(office);

    officeManager.setUro(manager, office, roleDao.getRoleByName(Role.PERSONNEL_ADMIN));
    officeManager.setUro(manager, office, roleDao.getRoleByName(Role.EMPLOYEE));

    properties.remove("personnelAdminPassword");
    properties.remove("adminPassword");

    try {
      properties.store(new FileOutputStream("conf/Wizard_Properties.conf"), "Wizard values file");
      Logger.info("Salvato file Wizard_Properties.conf");
    } catch (IOException e) {
      flash.error(e.getMessage());
    }

    redirect(Play.ctxPath + "/");
  }

  public static class WizardStep {
    public final int index;
    public final String name;
    public final String template;
    public boolean completed = false;

    WizardStep(String name, String template, int index) {
      this.name = name;
      this.template = template;
      this.index = index;
    }

    public static WizardStep of(String name, String template, int index) {
      return new WizardStep(name, template, index);
    }

    public void complete() {
      completed = true;
    }

  }

}
