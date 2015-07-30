package controllers;

import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.ExportToYaml;

import java.util.List;

import javax.inject.Inject;

import jobs.RemoveInvalidStampingsJob;
import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.StampType;
import models.enumerate.Parameter;
import models.enumerate.TimeAtWorkModifier;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

@With( {Resecure.class, RequestInit.class} )
public class Administration extends Controller {

	@Inject
	private static OfficeDao officeDao;
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

	
	public static void initializeRomanAbsences() {

		//StampType pausa pranzo
		StampType st = StampType.find("byCode", "pausaPranzo").first();
		if(st == null) {
			st = new StampType();
			st.code = "pausaPranzo";
			st.description = "Pausa pranzo";
			st.identifier = "pr";
			st.save();
		}

		AbsenceType absenceType = AbsenceType.find("byCode", "PEPE").first();
		if(absenceType==null) {
			// creare le assenze romane
			absenceType = new AbsenceType();
			absenceType.code = "PEPE";
			absenceType.description = "Permesso Personale";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}

		absenceType = AbsenceType.find("byCode", "RITING").first();
		if(absenceType==null) {
			absenceType = new AbsenceType();
			absenceType.code = "RITING";
			absenceType.description = "AUTORIZ.DIRIG.RITARDO.INGR.TUR";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}

		absenceType = AbsenceType.find("byCode", "661h").first();
		if(absenceType==null) {	
			absenceType = new AbsenceType();
			absenceType.code = "661h";
			absenceType.description = "PERM.ORARIO GRAVI MOTIVI";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}
		absenceType = AbsenceType.find("byCode", "09B").first();
		if(absenceType==null) {
			absenceType = new AbsenceType();
			absenceType.code = "09B";
			absenceType.description = "ORE DI  MALAT. O VIS.ME";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}
		absenceType = AbsenceType.find("byCode", "103").first();
		if(absenceType==null) {
			absenceType = new AbsenceType();
			absenceType.code = "103";
			absenceType.description = "Telelavoro";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}
		absenceType = AbsenceType.find("byCode", "91.").first();
		if(absenceType==null) {	
			absenceType = new AbsenceType();
			absenceType.code = "91.";
			absenceType.description = "RIPOSO COMPENSATIVO 1/3 L";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}
		absenceType = AbsenceType.find("byCode", "91CE").first();
		if(absenceType==null) {	
			absenceType = new AbsenceType();
			absenceType.code = "91CE";
			absenceType.description = "RIP. COMP.CHIUSURA ENTE";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}
		absenceType = AbsenceType.find("byCode", "182").first();
		if(absenceType==null) {
			absenceType = new AbsenceType();
			absenceType.code = "182";
			absenceType.description = "PERM ASSIST.PARENTI 2";
			absenceType.internalUse = true;
			absenceType.timeAtWorkModification = TimeAtWorkModifier.JustifyAllDay;
			absenceType.save();
		}

	}
	
	public static void initializePersons() {
		
		//Tutte le persone con contratto iniziato dopo alla data di inizializzazione
		// devono avere la inizializzazione al giorno prima.
		List<Person> persons = Person.findAll();
		for(Person person : persons) {
			
			//Configurazione office
			String dateInitUse = confGeneralManager.getFieldValue(Parameter.INIT_USE_PROGRAM, person.office);
			LocalDate initUse = new LocalDate(dateInitUse);
			
			//Contratto attuale
			Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
			
			if(contract.isPresent()) {
				if(contract.get().sourceDate == null && contract.get().beginContract.isBefore(initUse)) {
					Contract c = contract.get();
					c.sourceDate = initUse.minusDays(1);
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
	//private final static Logger log = LoggerFactory.getLogger(Administration.class);

	public static void utilities(){

		final List<Person> personList = personDao.list(
				Optional.<String>absent(),officeDao.getOfficeAllowed(Security.getUser().get()), 
				false, LocalDate.now(), LocalDate.now(), true)
				.list();

		render(personList);
	}

	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 */
	public static void fixPersonSituation(Long personId, int year, int month) {	
		LocalDate date = new LocalDate(year,month,1);
		Optional<Person> person = personId == -1 ? Optional.<Person>absent() : Optional.fromNullable(personDao.getPersonById(personId));
		consistencyManager.fixPersonSituation(person,Security.getUser(), date, false);
	}

	public static void buildYaml(){
		//general
		exportToYaml.buildAbsenceTypesAndQualifications(
				"db/import/absenceTypesAndQualifications"+DateTime.now().toString("dd-MM-HH:mm")+".yml");

		exportToYaml.buildCompetenceCodes(
				"db/import/competenceCode"+DateTime.now().toString("dd-MM-HH:mm")+".yml");

		exportToYaml.buildVacationCodes(
				"db/import/vacationCode"+DateTime.now().toString("dd-MM-HH:mm")+".yml");

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
	
	public static void deleteUncoupledStampings(@Required List<Long> peopleId,
			@Required LocalDate begin,LocalDate end){
	
		if (validation.hasErrors()){
			params.flash(); 
			utilities();
		}
	
		if(end == null){
			end = begin;
		}
	
		List<Person> people = Lists.newArrayList();
	
		for(Long id : peopleId){
			people.add(personDao.getPersonById(id));
		}
	
		for(Person person : people){
			new RemoveInvalidStampingsJob(person, begin, end).afterRequest();
		}
	
		flash.success("Avviati Job per la rimozione delle timbrature non valide per %s", people);
		utilities();
	}

}
