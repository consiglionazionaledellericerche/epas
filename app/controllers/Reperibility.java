/**
 * 
 */
package controllers;

import static play.modules.pdf.PDF.renderPDF;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.joda.time.LocalDate;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import controllers.Resecure.BasicAuth;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import it.cnr.iit.epas.JsonReperibilityChangePeriodsBinder;
import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;
import manager.AbsenceManager;
import manager.ReperibilityManager;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.User;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;
import play.Logger;
import play.data.binding.As;
import play.data.validation.Required;
import play.i18n.Messages;
import play.modules.pdf.PDF.Options;
import play.mvc.Controller;
import play.mvc.With;


/**
 * @author cristian
 *
 */
@With(Resecure.class)
public class Reperibility extends Controller {
	
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(Shift.class);

	private static String codFr = "207";
	private static String codFs = "208";

	@Inject
	private static PersonDao personDao;
	@Inject
	private static PersonReperibilityDayDao personReperibilityDayDao;
	@Inject
	private static ReperibilityManager reperibilityManager;
	@Inject
	private static AbsenceDao absenceDao;
	@Inject
	private static AbsenceManager absenceManager;
	@Inject
	private static CompetenceCodeDao competenceCodeDao;
	@Inject
	private static CompetenceDao competenceDao;

	/*
	 * @author arianna
	 * Restituisce la lista dei reperibili attivi al momento di un determinato tipo
	 */
	@BasicAuth
	public static void personList() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.devel.iit.cnr.it");

		Long type = Long.parseLong(params.get("type"));
		Logger.debug("Esegue la personList con type=%s", type);

		List<Person> personList = personDao.getPersonForReperibility(type);
		Logger.debug("Reperibility personList called, found %s reperible person", personList.size());
		render(personList);
	}

	/**
	 * @author cristian, arianna
	 * Fornisce i periodi di reperibilità del personale reperibile di tipo 'type'
	 * nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
	 * 
	 * per provarlo: curl -H "Accept: application/json" http://localhost:9001/reperibility/1/find/2012/11/26/2013/01/06
	 */
	@BasicAuth
	public static void find() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		// reperibility type validation
		Long type = Long.parseLong(params.get("type"));
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}

		PersonReperibilityType prt = personReperibilityDayDao.getPersonReperibilityTypeById(type);

		// date interval construction
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(from, to, reperibilityType, Optional.<PersonReperibility>absent());
		//		PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND prd.reperibilityType = ? ORDER BY prd.date", from, to, reperibilityType).fetch();

		Logger.debug("Reperibility find called from %s to %s, found %s reperibility days", from, to, reperibilityDays.size());
		// Manager ReperibilityManager called to find out the reperibilityPeriods
		List<ReperibilityPeriod> reperibilityPeriods = reperibilityManager.getPersonReperibilityPeriods(reperibilityDays, prt);
		Logger.debug("Find %s reperibilityPeriods. ReperibilityPeriods = %s", reperibilityPeriods.size(), reperibilityPeriods);

		render(reperibilityPeriods);
	}


	/**
	 * @author arianna
	 * Fornisce la lista del personale reperibile di tipo 'type' 
	 * nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
	 * 
	 */
	@BasicAuth
	public static void who() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		List<Person> personList = new ArrayList<Person>();

		// reperibility type validation
		Long type = Long.parseLong(params.get("type"));
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}

		// date interval construction
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = 
				personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(
						from, to, reperibilityType, Optional.<PersonReperibility>absent());

		Logger.debug("Reperibility who called from %s to %s, found %s reperibility days", from, to, reperibilityDays.size());

		personList = reperibilityManager.getPersonsFromReperibilityDays(reperibilityDays);
		Logger.debug("trovati %s reperibili: %s", personList.size(), personList);

		render(personList);
	}


	/**
	 * @author arianna
	 * Legge le assenze dei reperibili di una determinata tipologia in un dato intervallo di tempo
	 * (portale sistorg)
	 */
	@BasicAuth
	public static void absence() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		Logger.debug("Sono nella absebce");

		Long type = Long.parseLong(params.get("type"));

		PersonReperibilityType repType = PersonReperibilityType.findById(type);
		if (repType == null) {
			notFound(String.format("PersonReperibilityType type = %s doesn't exist", type));			
		}

		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		// read the reperibility person list 
		List<Person> personList = personDao.getPersonForReperibility(type);
		Logger.debug("Reperibility personList called, found %s reperible person of type %s", personList.size(), type);

		// Lists of absence for a single reperibility person and for all persons
		List<Absence> absencePersonReperibilityDays = new ArrayList<Absence>();

		// List of absence periods
		List<AbsenceReperibilityPeriod> absenceReperibilityPeriods = new ArrayList<AbsenceReperibilityPeriod>();

		if (personList.size() == 0) {
			render(absenceReperibilityPeriods);
			return;
		}

		absencePersonReperibilityDays = absenceDao.getAbsenceForPersonListInPeriod(personList, from, to);

		Logger.debug("Trovati %s giorni di assenza", absencePersonReperibilityDays.size());

		// get the absent reperibility periods from the absent days
		absenceReperibilityPeriods = reperibilityManager.getAbsentReperibilityPeriodsFromAbsentReperibilityDays(absencePersonReperibilityDays, repType);

		Logger.debug("Find %s absenceReperibilityPeriod. AbsenceReperibilityPeriod = %s", absenceReperibilityPeriods.size(), absenceReperibilityPeriods.toString());
		render(absenceReperibilityPeriods);
	}


	/**
	 * @author arianna
	 * Restituisce la lista delle persone reperibili assenti di una determinata tipologia in un dato intervallo di tempo
	 * (portale sistorg)
	 */
	@BasicAuth
	public static void whoIsAbsent() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		List<Person> absentPersonsList = new ArrayList<Person>();

		Long type = Long.parseLong(params.get("type"));

		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		// read the reperibility person list 
		List<Person> personList = personDao.getPersonForReperibility(type);
		Logger.debug("Reperibility personList called, found %s reperible person of type %s", personList.size(), type);

		// Lists of absence for a single reperibility person and for all persons
		List<Absence> absencePersonReperibilityDays = new ArrayList<Absence>();

		if (personList.size() == 0) {
			render(personList);
			return;
		}

		//		absencePersonReperibilityDays = JPA.em().createQuery("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date BETWEEN :from AND :to AND pd.person IN (:personList) ORDER BY pd.person.id, pd.date")
		//			.setParameter("from", from)
		//			.setParameter("to", to)
		//			.setParameter("personList", personList)
		//			.getResultList();

		absencePersonReperibilityDays = absenceDao.getAbsenceForPersonListInPeriod(personList, from, to);		
		Logger.debug("Trovati %s giorni di assenza", absencePersonReperibilityDays.size());

		absentPersonsList = absenceManager.getPersonsFromAbsentDays(absencePersonReperibilityDays);

		Logger.debug("Find %s person. absentPersonsList = %s", absentPersonsList.size(), absentPersonsList.toString());
		render(absentPersonsList);
	}


	/**
	 * @author cristian, arianna
	 * Aggiorna le informazioni relative alla Reperibilità del personale
	 * 
	 * Per provarlo è possibile effettuare una chiamata JSON come questa:
	 * 	$  curl -H "Content-Type: application/json" -X PUT \
	 * 			-d '[ {"id" : "49","start" : 2012-12-05,"end" : "2012-12-10", "reperibility_type_id" : "1"}, { "id" : "139","start" : "2012-12-12" , "end" : "2012-12-14", "reperibility_type_id" : "1" } , { "id" : "139","start" : "2012-12-17","end" : "2012-12-18", "reperibility_type_id" : "1" } ]' \ 
	 * 			http://localhost:9000/reperibility/1/update/2012/12
	 * 
	 * @param body
	 */
	@BasicAuth
	public static void update(Long type, Integer year, Integer month, @As(binder=JsonReperibilityPeriodsBinder.class) ReperibilityPeriods body) {

		Logger.debug("update: Received reperebilityPeriods %s", body);	
		if (body == null) {
			badRequest();	
		}

		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		PersonReperibilityType reperibilityType = personReperibilityDayDao.getPersonReperibilityTypeById(type);
		if (reperibilityType == null) {
			throw new IllegalArgumentException(String.format("ReperibilityType id = %s doesn't exist", type));			
		}

		//Conterrà i giorni del mese che devono essere attribuiti a qualche reperibile 
		Set<Integer> repDaysOfMonthToRemove = new HashSet<Integer>();	

		repDaysOfMonthToRemove = reperibilityManager.savePersonReperibilityDaysFromReperibilityPeriods(reperibilityType, year, month, body.periods);

		Logger.debug("Giorni di reperibilità da rimuovere = %s", repDaysOfMonthToRemove);

		int deletedRep = reperibilityManager.deleteReperibilityDaysFromMonth(reperibilityType, year, month, repDaysOfMonthToRemove);
		log.info("Deleted {} days, reperibilityType={}, {}/{}, repDaysOfMonthToRemove={}", 
			deletedRep, reperibilityType, year, month, repDaysOfMonthToRemove);
	}


	/**
	 * @author arianna
	 * Scambia due periodi di reperibilità di due persone reperibili diverse
	 * 
	 * Per provarlo è possibile effettuare una chiamata JSON come questa:
	 * 	$  curl -H "Content-Type: application/json" -X PUT \
	 * 			-d '[ {"mail_req" : "ruberti@iit.cnr.it", "mail_sub" : "lorenzo.rossi@iit.cnr.it", "req_start_date" : "2012-12-10", "req_end_date" : "2012-12-10", "sub_start_date" : "2012-12-10", "sub_end_date" : "2012-12-10"} ]' \ 
	 * 			http://scorpio.nic.it:9001/reperibility/1/changePeriods
	 * 
	 * @param body
	 */
	@BasicAuth
	public static void changePeriods(Long type, @As(binder=JsonReperibilityChangePeriodsBinder.class) ReperibilityPeriods body) {

		Logger.debug("update: Received reperebilityPeriods %s", body);	
		if (body == null) {
			badRequest();	
		}

		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		PersonReperibilityType reperibilityType = personReperibilityDayDao.getPersonReperibilityTypeById(type);
		if (reperibilityType == null) {
			throw new IllegalArgumentException(String.format("ReperibilityType id = %s doesn't exist", type));			
		}

		Boolean changed = reperibilityManager.changeTwoReperibilityPeriods(reperibilityType, body.periods);

		if (changed) {
			Logger.info("Periodo di reperibilità cambiato con successo!");
		} else {
			Logger.info("Il cambio di reperibilità non è stato effettuato");
		}

	}


	/**
	 * @author arianna, cristian
	 * crea il file PDF con il calendario annuale delle reperibilità di tipi 'type' per l'anno 'year'
	 * (portale sistorg)
	 */
	@BasicAuth
	public static void exportYearAsPDF() {
		int year = params.get("year", Integer.class);
		Long reperibilityId = params.get("type", Long.class);

		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		PersonReperibilityType reperibilityType = personReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}

		// build the reperibility calendar 
		List<Table<Person, Integer, String>> reperibilityMonths = new ArrayList<Table<Person, Integer, String>>();
		reperibilityMonths = reperibilityManager.buildYearlyReperibilityCalendar(year, reperibilityType);

		// build the reperibility summary report
		Table<Person, String, Integer> reperibilitySumDays = HashBasedTable.<Person, String, Integer>create();
		reperibilitySumDays = reperibilityManager.buildYearlyReperibilityReport(reperibilityMonths);
		Logger.info("Creazione del documento PDF con il calendario annuale delle reperibilità per l'anno %s", year);


		LocalDate firstOfYear = new LocalDate(year, 1, 1);
		Options options = new Options();
		options.pageSize = IHtmlToPdfTransformer.A4L;
		renderPDF(options, year, firstOfYear, reperibilityMonths, reperibilitySumDays);
	}


	/**
	 * @author arianna
	 * restituisce una tabella con le eventuali inconsistenze tra le timbrature dei reperibili di un certo tipo e i
	 * turni di reperibilità svolti in un determinato periodo di tempo
	 * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence], List<'gg MMM'>)
	 */
	public static Table<Person, String, List<String>> getInconsistencyTimestamps2Reperibilities (Long reperibilityId, LocalDate startDate, LocalDate endDate) {
		// for each person contains days with absences and no-stamping  matching the reperibility days 
		Table<Person, String, List<String>> inconsistentAbsence = TreeBasedTable.<Person, String, List<String>>create();				

		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		PersonReperibilityType reperibilityType = personReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}

		List<PersonReperibilityDay> personReperibilityDays = 
				personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(startDate, endDate, reperibilityType, Optional.<PersonReperibility>absent());

		inconsistentAbsence = reperibilityManager.getReperibilityInconsistenceAbsenceTable(personReperibilityDays, startDate, endDate);

		return inconsistentAbsence;
	}


	/*
	 * @author arianna
	 * crea il file PDF con il resoconto mensile delle reperibilità di tipo 'type' per
	 * il mese 'month' dell'anno 'year'
	 * Segnala le eventuali inconsistenze con le assenze o le mancate timbrature
	 * (portale sistorg)
	 */
	@BasicAuth
	public static void exportMonthAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
		Long reperibilityId = params.get("type", Long.class);

		LocalDate today = new LocalDate();

		// for each person contains the number of rep days fr o fs (feriali o festivi)
		Table<Person, String, Integer> reperibilitySumDays = TreeBasedTable.<Person, String, Integer>create();

		// for each person contains the list of the rep periods divided by fr o fs
		Table<Person, String, List<String>> reperibilityDateDays = TreeBasedTable.<Person, String, List<String>>create();

		// for each person contains days with absences and no-stamping  matching the reperibility days 
		Table<Person, String, List<String>> inconsistentAbsence = TreeBasedTable.<Person, String, List<String>>create();				

		// get the Competence code for the reperibility working or non-working days  
		//CompetenceCode competenceCodeFS = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFs).first();
		CompetenceCode competenceCodeFS = competenceCodeDao.getCompetenceCodeByCode(codFs); 

		//CompetenceCode competenceCodeFR = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFr).first();
		CompetenceCode competenceCodeFR = competenceCodeDao.getCompetenceCodeByCode(codFr);

		Logger.debug("Creazione dei  competenceCodeFS competenceCodeFR %s/%s", competenceCodeFS, competenceCodeFR);

		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		PersonReperibilityType reperibilityType = personReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}

		// get all the reperibility of a certain type in a certain month
		LocalDate firstOfMonth = new LocalDate(year, month, 1);

		List<PersonReperibilityDay> personReperibilityDays = 
				personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), reperibilityType, Optional.<PersonReperibility>absent());

		Logger.debug("dimensione personReperibilityDays = %s", personReperibilityDays.size());

		// update the reperibility days in the DB
		int updatedCompetences = reperibilityManager.updateDBReperibilityCompetences(personReperibilityDays, year, month);
		Logger.debug("Salvate o aggiornate %d competences", updatedCompetences);

		// builds the table with the summary of days and reperibility periods description
		// reading data from the Competence table in the DB
		//List<Competence> frCompetences = Competence.find("SELECT com FROM Competence com JOIN com.person p WHERE p.reperibility.personReperibilityType = ? AND com.year = ? AND com.month = ? AND com.competenceCode = ? ORDER by p.surname", reperibilityType, year, month, competenceCodeFR).fetch();
		List<Competence> frCompetences = competenceDao.getCompetenceInReperibility(reperibilityType, year, month, competenceCodeFR);
		Logger.debug("Trovate %d competences di tipo %s nel mese %d/%d", frCompetences.size(), reperibilityType,  month, year);

		// update  reports for the approved days and reasons for the working days
		reperibilityManager.updateReperibilityDaysReportFromCompetences(reperibilitySumDays, frCompetences);
		reperibilityManager.updateReperibilityDatesReportFromCompetences(reperibilityDateDays, frCompetences);


		// builds the table with the summary of days and reperibility periods description
		// reading data from the Competence table in the DB
		//List<Competence> fsCompetences = Competence.find("SELECT com FROM Competence com JOIN com.person p WHERE p.reperibility.personReperibilityType = ? AND com.year = ? AND com.month = ? AND com.competenceCode = ? ORDER by p.surname", reperibilityType, year, month, competenceCodeFS).fetch();
		List<Competence> fsCompetences = competenceDao.getCompetenceInReperibility(reperibilityType, year, month, competenceCodeFS);
		Logger.debug("Trovate %d competences di tipo %s nel mese %d/%d", fsCompetences.size(), reperibilityType,  month, year);

		// update  reports for the approved days and reasons for the holidays 
		reperibilityManager.updateReperibilityDaysReportFromCompetences(reperibilitySumDays, fsCompetences);
		reperibilityManager.updateReperibilityDatesReportFromCompetences(reperibilityDateDays, fsCompetences);

		// get the table with the absence and no stampings inconsistency 
		inconsistentAbsence = reperibilityManager.getReperibilityInconsistenceAbsenceTable(personReperibilityDays, firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue());

		Logger.info("Creazione del documento PDF con il resoconto delle reperibilità per il periodo %s/%s Fs=%s Fr=%s", firstOfMonth.plusMonths(0).monthOfYear().getAsText(), firstOfMonth.plusMonths(0).year().getAsText(), codFs, codFr);

		String cFr = codFr;
		String cFs = codFs;
		String thNoStamp = Messages.get("PDFReport.thNoStampings");
		String thAbs = Messages.get("PDFReport.thAbsences");

		renderPDF(today, firstOfMonth, reperibilitySumDays, reperibilityDateDays, inconsistentAbsence, cFs, cFr, thNoStamp, thAbs);

	}

	@BasicAuth
	public static void iCal(@Required Long type, @Required int year, Long personId) {
		
		if (validation.hasErrors()) {
			badRequest("Parametri mancanti. " + validation.errors());
		}
		Optional<User> currentUser = Security.getUser();

		response.accessControl("*");
		
		PersonReperibilityType reperibilityType = personReperibilityDayDao.getPersonReperibilityTypeById(type);
		if (reperibilityType == null) {
				notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		ImmutableList<Person> canAccess =  
				ImmutableList.<Person>builder()
					.addAll(personDao.getPersonForReperibility(type))
					.add(reperibilityType.supervisor).build();
		
		
		if (!currentUser.isPresent() || currentUser.get().person == null 
				|| !canAccess.contains(currentUser.get().person)) {
			log.debug("Accesso all'iCal dei turni non autorizzato: Type = {}, Current User = {}, "
					+ "canAccess = {}", 
				type, currentUser.get(), canAccess, currentUser.get());
			unauthorized();
		}
		
	
		
		try {
			
			Optional<Calendar> calendar = reperibilityManager.createCalendar(type, Optional.fromNullable(personId), year);
			if (!calendar.isPresent()) {
				notFound(String.format("No person associated to a reperibility of type = %s", reperibilityType));
			}
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			CalendarOutputter outputter = new CalendarOutputter();
			outputter.output(calendar.get(), bos);
			
			response.setHeader("Content-Type", "application/ics");
			InputStream is = new ByteArrayInputStream(bos.toByteArray());
			renderBinary(is,"reperibilitaRegistro.ics");
			bos.close();
			is.close();
		} catch (IOException e) {
			log.error("Io exception building ical", e);
			error("Io exception building ical");
		} catch (ValidationException e) {
			log.error("Validation exception generating ical", e);
			error("Validation exception generating ical");
		}
	}

}
