package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.ExportToYaml;

import lombok.extern.slf4j.Slf4j;

import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.PersonDayManager;
import manager.SecureManager;

import models.AbsenceType;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.Parameter;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

import javax.inject.Inject;


@Slf4j
@With({Resecure.class, RequestInit.class})
public class Administration extends Controller {

  @Inject
  private static SecureManager secureManager;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static ExportToYaml exportToYaml;
  @Inject
  private static CompetenceUtility competenceUtility;
  @Inject
  private static ConfGeneralManager confGeneralManager;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static ContractManager contractManager;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonDayManager personDayManager;

  public static void initializeRomanAbsences() {

    //StampType pausa pranzo
    StampType st = StampType.find("byCode", "pausaPranzo").first();
    if (st == null) {
      st = new StampType();
      st.code = "pausaPranzo";
      st.description = "Pausa pranzo";
      st.identifier = "pr";
      st.save();
    }

    AbsenceType absenceType = AbsenceType.find("byCode", "PEPE").first();
    if (absenceType == null) {
      // creare le assenze romane
      absenceType = new AbsenceType();
      absenceType.code = "PEPE";
      absenceType.description = "Permesso Personale";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "RITING").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "RITING";
      absenceType.description = "AUTORIZ.DIRIG.RITARDO.INGR.TUR";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }

    absenceType = AbsenceType.find("byCode", "661h").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "661h";
      absenceType.description = "PERM.ORARIO GRAVI MOTIVI";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "09B").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "09B";
      absenceType.description = "ORE DI  MALAT. O VIS.ME";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "103").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "103";
      absenceType.description = "Telelavoro";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "91.").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "91.";
      absenceType.description = "RIPOSO COMPENSATIVO 1/3 L";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "91CE").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "91CE";
      absenceType.description = "RIP. COMP.CHIUSURA ENTE";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }
    absenceType = AbsenceType.find("byCode", "182").first();
    if (absenceType == null) {
      absenceType = new AbsenceType();
      absenceType.code = "182";
      absenceType.description = "PERM ASSIST.PARENTI 2";
      absenceType.internalUse = true;
      absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.AllDay;
      absenceType.save();
    }

  }

  public static void initializePersons() {

    //Tutte le persone con contratto iniziato dopo alla data di inizializzazione
    // devono avere la inizializzazione al giorno prima.
    List<Person> persons = Person.findAll();
    for (Person person : persons) {

      //Configurazione office
      LocalDate initUse = confGeneralManager.getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, person.office).orNull();

      //Contratto attuale
      Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();

      if (contract.isPresent()) {
        if (contract.get().sourceDateResidual == null && contract.get().beginDate.isBefore(initUse)) {
          Contract c = contract.get();
          c.sourceDateResidual = initUse.minusDays(1);
          c.sourcePermissionUsed = 0;
          c.sourceRecoveryDayUsed = 0;
          c.sourceRemainingMealTicket = 0;
          c.sourceRemainingMinutesCurrentYear = 6000;
          c.sourceRemainingMinutesLastYear = 0;
          c.sourceVacationCurrentYearUsed = 0;
          c.sourceVacationLastYearUsed = 0;
          c.save();
        }
      }
    }
  }

  public static void utilities() {

    final List<Person> personList = personDao.list(
            Optional.<String>absent(),
            secureManager.officesWriteAllowed(Security.getUser().get()),
            false, LocalDate.now(), LocalDate.now(), true)
            .list();

    render(personList);
  }

  /**
   * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
   *
   * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone
   * @param year     l'anno dal quale far partire il fix
   * @param month    il mese dal quale far partire il fix
   */
  public static void fixPersonSituation(Long personId, int year, int month, boolean onlyRecap) {

    LocalDate date = new LocalDate(year, month, 1);

    Optional<Person> person = personId == null ? Optional.<Person>absent()
            : Optional.fromNullable(personDao.getPersonById(personId));

    consistencyManager.fixPersonSituation(person, Security.getUser(), date, false, onlyRecap);

    flash.success("Esecuzione terminata");

    utilities();
  }

  public static void buildYaml() {
    //general
    exportToYaml.buildAbsenceTypesAndQualifications(
            "db/import/absenceTypesAndQualifications" + DateTime.now().toString("dd-MM-HH:mm") + ".yml");

    exportToYaml.buildCompetenceCodes(
            "db/import/competenceCode" + DateTime.now().toString("dd-MM-HH:mm") + ".yml");

    exportToYaml.buildVacationCodes(
            "db/import/vacationCode" + DateTime.now().toString("dd-MM-HH:mm") + ".yml");

    //		exportToYaml.buildVacationCodes("conf/vacationCodes.yml");

    //		Yaml yaml = new Yaml();

    //		exportToYaml.writeToYamlFile("Users"+DateTime.now().toString("dd-MM-HH:mm")+".yml", yaml.dump(User.findAll()));
    //		exportToYaml.writeToYamlFile("Permission"+DateTime.now().toString("dd-MM-HH:mm")+".yml", yaml.dump(Permission.findAll()));
    //		exportToYaml.writeToYamlFile("Roles"+DateTime.now().toString("dd-MM-HH:mm")+".yml", yaml.dump(Role.findAll()));
  }

  public static void updateExceedeMinInCompetenceTable() {
    competenceUtility.updateExceedeMinInCompetenceTable();
    renderText("OK");
  }

  public static void deleteUncoupledStampings(List<Long> peopleId,
                                              @Required LocalDate begin, LocalDate end, boolean forAll) {

    if (validation.hasErrors()) {
      params.flash();
      utilities();
    }

    if (end == null) {
      end = begin;
    }

    List<Person> people = Lists.newArrayList();
    if (!forAll) {
      for (Long id : peopleId) {
        people.add(personDao.getPersonById(id));
      }

    } else {
      // Tutte le persone attive nella finestra speficificata.
      List<Contract> contracts = contractDao
              .getActiveContractsInPeriod(begin, Optional.fromNullable(end));
      for (Contract contract : contracts) {
        people.add(contract.person);
      }
    }

    for (Person person : people) {


      person = Person.findById(person.id);

      log.info("Rimozione timbrature disaccoppiate per {} ...", person.fullName());
      List<PersonDay> persondays = personDayDao
              .getPersonDayInPeriod(person, begin, Optional.of(end));
      int count = 0;
      for (PersonDay pd : persondays) {
        personDayManager.computeValidStampings(pd);

        for (Stamping stamping : pd.stampings) {
          if (!stamping.valid) {
            stamping.delete();
            count++;
          }
        }
      }

      log.info("... rimosse {} timbrature disaccoppiate.", count);
    }

    flash.success("Esecuzione terminata");

    utilities();
  }

  public static void fixVacationPeriods(LocalDate from) {

    List<Contract> contracts = contractDao
            .getActiveContractsInPeriod(from, Optional.<LocalDate>absent());

    for (Contract contract : contracts) {
      contractManager.buildVacationPeriods(contract);

      log.info("Il contratto di {} iniziato il {} non è stato ripristinato con i piani ferie corretti.",
              contract.person.fullName(), contract.beginDate);
    }

    utilities();
  }

}
