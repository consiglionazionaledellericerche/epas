package controllers;

import helpers.ModelQuery.SimpleResults;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import manager.CompetenceManager;
import manager.recaps.PersonCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecapFactory;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Permission;
import models.Person;
import models.TotalOvertime;
import models.User;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PermissionDao;
import dao.PersonDao;
import dao.wrapper.IWrapperCompetenceCode;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class} )
public class Competences extends Controller{

	private final static Logger log = LoggerFactory.getLogger(Competences.class);

	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static PersonMonthCompetenceRecapFactory personMonthCompetenceRecapFactory;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static CompetenceManager competenceManager;
	@Inject
	private static SecurityRules rules;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static CompetenceDao competenceDao;
	@Inject
	private static CompetenceCodeDao competenceCodeDao;

	public static void competences(int year, int month) {

		Optional<User> user = Security.getUser();

		if( ! user.isPresent() || user.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}

		Person person = user.get().person;

		Optional<Contract> contract = wrapperFactory.create(person)
				.getLastContractInMonth(year, month);

		if(! contract.isPresent() ) {
			flash.error("Nessun contratto attivo nel mese.");
			renderTemplate("Application/indexAdmin.html");
		}

		PersonMonthCompetenceRecap personMonthCompetenceRecap =
				personMonthCompetenceRecapFactory.create(contract.get(), month, year);

		render(personMonthCompetenceRecap, person, year, month);

	}

	public static void showCompetences(Integer year, Integer month, Long officeId, 
			String name, String codice, Integer page){

		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());

		if(officeId == null) {
			
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione "
						+ "delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}
		
		//Redirect in caso di mese futuro
		LocalDate today = LocalDate.now();
		if (today.getYear() == year && month > today.getMonthOfYear()) {
			flash.error("Impossibile accedere a situazione futura, "
					+ "redirect automatico a mese attuale");
			showCompetences(year, today.getMonthOfYear(), officeId, name, codice, page);
		}

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		rules.checkIfPermitted(office);
		if (page==null) {
			page = 0;
		}
		
		boolean editCompetence = Security.hasPermissionOnOffice(office, Security.EDIT_COMPETENCE);
		renderArgs.put("editCompetence", editCompetence);

		////////////////////////////////////////////////////////////////////////
		// La lista dei codici competenceCode da visualizzare nella select
		// Ovver: I codici attualmente attivi per almeno un dipendente di quell'office
		Set<CompetenceCode> competenceCodeList = Sets.newHashSet();
		competenceCodeList.addAll(competenceDao.activeCompetenceCode(office));
		
		if (competenceCodeList.size() == 0) {
			flash.error("Per visualizzare la sezione Competenze è necessario "
					+ "abilitare almeno un codice competenza ad un dipendente.");
			Competences.enabledCompetences(officeId, null);
		}
		
        ////////////////////////////////////////////////////////////////////////
		// Il codice della richiesta ( check esistenza )
		IWrapperCompetenceCode competenceCode = null;
		if (codice == null || codice == "") {
			competenceCode = wrapperFactory.create(competenceCodeList.iterator().next());
		} else {
			competenceCode = wrapperFactory.create(competenceCodeDao
					.getCompetenceCodeByCode(codice));
			Preconditions.checkNotNull(competenceCode.getValue());
		}	

        ////////////////////////////////////////////////////////////////////////
        // Le persone che hanno quella competence attualmente abilitata
		SimpleResults<Person> simpleResults = personDao.listForCompetence(competenceCode.getValue(),
				Optional.fromNullable(name), Sets.newHashSet(office), false, 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
				Optional.<Person>absent());

		List<IWrapperPerson> activePersons = FluentIterable
				.from(simpleResults.paginated(page).getResults())
				.transform(wrapperFunctionFactory.person()).toList();

		for(IWrapperPerson p : activePersons){
			Competence competence = null;
			for(CompetenceCode c : p.getValue().competenceCode){
				Optional<Competence> comp = competenceDao.getCompetence(p.getValue(), year, month, c);
				if(!comp.isPresent()){
					competence = new Competence(p.getValue(), c, year, month);
					competence.valueApproved = 0;
					competence.save();
				}

			}
		}
		
		// TODO: mancano da visualizzare le competence assegnate nel mese a quelle
		// persone che non hanno più il relativo codice abilitato.
		
		List<String> code = competenceManager.populateListWithOvertimeCodes();		

		List<Competence> competenceList = competenceDao.getCompetences(Optional.<Person>absent(),year, month, code, office, false);
		int totaleOreStraordinarioMensile = competenceManager.getTotalMonthlyOvertime(competenceList);

		List<Competence> competenceYearList = competenceDao.getCompetences(Optional.<Person>absent(),year, month, code, office, true);		
		int totaleOreStraordinarioAnnuale = competenceManager.getTotalYearlyOvertime(competenceYearList);

		List<TotalOvertime> total = competenceDao.getTotalOvertime(year, office);				
		int totaleMonteOre = competenceManager.getTotalOvertime(total);		

		render(year, month, office, offices, activePersons, totaleOreStraordinarioMensile, totaleOreStraordinarioAnnuale, 
				totaleMonteOre, simpleResults, name, codice, competenceCodeList, competenceCode);

	}

	public static void updateCompetence(long pk, String name, Integer value){
		final Competence competence = competenceDao.getCompetenceById(pk);

		notFoundIfNull(competence);
		if (validation.hasErrors()) {
			error(Messages.get(Joiner.on(",").join(validation.errors())));
		}
		rules.checkIfPermitted(competence.person.office);

		log.info("Anno competenza: {} Mese competenza: {}", 
				new Object[]{competence.year, competence.month});

		competence.valueApproved = value;

		log.info("value approved before = {}", 
				new Object[]{competence.valueApproved});

		competence.save();

		log.info("saved id={} (person={}) code={} (value={})", 
				new Object[] { competence.id, competence.person, 
				competence.competenceCode.code, competence.valueApproved} );

		renderText("ok");
	}

	public static void manageCompetenceCode(){

		List<CompetenceCode> compCodeList = competenceCodeDao.getAllCompetenceCode();
		render(compCodeList);
	}

	public static void insertCompetenceCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CompetenceCode code = new CompetenceCode();
		render(code);
	}

	public static void edit(Long competenceCodeId){
		//		FIXME decidere se permetterlo all'admin, ed eventualmente sistemare il controllo, o il permesso
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CompetenceCode code = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
		render(code);
	}

	public static void save(Long competenceCodeId){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		String codice = params.get("codice");
		String descrizione = params.get("descrizione");
		String codiceAtt = params.get("codiceAttPres");
		if(competenceManager.setNewCompetenceCode(competenceCodeId, codice, descrizione, codiceAtt)){
			flash.success(String.format("Codice %s aggiunto con successo", codice));
		}
		else{
			flash.error(String.format("Il codice competenza %s è già presente nel database. Cambiare nome al codice.", codice));
		}
		Application.indexAdmin();
	}

	public static void totalOvertimeHours(int year, Long officeId){

		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());		
		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);

		rules.checkIfPermitted(office);

		List<TotalOvertime> totalList = competenceDao.getTotalOvertime(year, office);
		int totale = competenceManager.getTotalOvertime(totalList);		

		render(totalList, totale, year, office, offices);
	}

	public static void saveOvertime(Integer year, String numeroOre, Long officeId){

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);

		rules.checkIfPermitted(office);
		if(competenceManager.saveOvertime(year, numeroOre, officeId)){
			flash.success(String.format("Aggiornato monte ore per l'anno %s", year));
		}
		else{
			flash.error("Inserire il segno (+) o (-) davanti al numero di ore da aggiungere (sottrarre)");
		}		

		Competences.totalOvertimeHours(year, officeId);
	}



	/**
	 * funzione che ritorna la tabella contenente le competenze associate a ciascuna persona
	 */
	public static void enabledCompetences(Long officeId, String name){

		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		rules.checkIfPermitted(office);
		LocalDate date = new LocalDate();		
		SimpleResults<Person> simpleResults = personDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(office), 
				false, date, date.dayOfMonth().withMaximumValue(), true);

		List<Person> personList = simpleResults.list();		
		Table<Person, String, Boolean> tableRecapCompetence = competenceManager.getTableForEnabledCompetence(personList);		
		int month = date.getMonthOfYear();
		int year = date.getYear();
		render(tableRecapCompetence, month, year, office, offices, simpleResults, name);
	}

	/**
	 * 
	 * @param personId render della situazione delle competenze per la persona nella form updatePersonCompetence
	 */
	public static void updatePersonCompetence(Long personId){

		if(personId == null){

			flash.error("Persona inesistente");
			Application.indexAdmin();
		}		
		Person person = personDao.getPersonById(personId);
		rules.checkIfPermitted(person.office);
		PersonCompetenceRecap pcr = new PersonCompetenceRecap(person,competenceCodeDao.getAllCompetenceCode());

		render(pcr,person);
	}

	/**
	 *  salva la nuova configurazione di competenze per la persona
	 */
	public static void saveNewCompetenceConfiguration(Long personId, Map<String, Boolean> competence){
		final Person person = personDao.getPersonById(personId);
		notFoundIfNull(person);
		rules.checkIfPermitted(person.office);

		List<CompetenceCode> competenceCode = competenceCodeDao.getAllCompetenceCode();
		if(competenceManager.saveNewCompetenceEnabledConfiguration(competence, competenceCode, person))
			flash.success(String.format("Aggiornate con successo le competenze per %s %s", person.name, person.surname));
		Competences.enabledCompetences( person.office.id, null);

	}


	public static void exportCompetences(){
		render();
	}	

	public static void getOvertimeInYear(int year) throws IOException{

		Office office = Security.getUser().get().person.office;
		SimpleResults<Person> simpleResults = personDao.listForCompetence(competenceCodeDao.getCompetenceCodeByCode("S1"), 
				Optional.fromNullable(""), 
				Sets.newHashSet(office), 
				false, 
				new LocalDate(year, 1, 1), 
				new LocalDate(year, 12, 1).dayOfMonth().withMaximumValue(),
				Optional.<Person>absent());

		List<Person> personList = simpleResults.list();
		FileInputStream inputStream = competenceManager.getOvertimeInYear(year, personList);
		renderBinary(inputStream, "straordinari"+year+".csv");
	}

	/**
	 * TODO: implementare un metodo nel manager nel quale spostare la business logic
	 * di questa azione.
	 * 
	 * @param officeId
	 * @param year
	 * @param onlyDefined true se si vuole applicare il calcolo ai soli tempi determinati
	 */
	public static void approvedCompetenceInYear(int year, boolean onlyDefined, Long officeId) {

		
		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());
		Preconditions.checkState(!offices.isEmpty());
		
		Office office;
		if(officeId != null) {
			office = officeDao.getOfficeById(officeId);
		} else {
			office = offices.iterator().next();
		}

		rules.checkIfPermitted(office);

		Set<Person> personSet = Sets.newHashSet();

		Map<CompetenceCode, Integer> totalValueAssigned = Maps.newHashMap();

		Map<Person, Map<CompetenceCode, Integer>> mapPersonCompetenceRecap = Maps.newHashMap();

		List<Competence> competenceInYear = competenceDao
				.getCompetenceInYear(year, Optional.fromNullable(office));

		for (Competence competence: competenceInYear) {

			//Filtro tipologia del primo contratto nel mese della competenza
			if (onlyDefined) {
				IWrapperPerson wperson = wrapperFactory.create(competence.person);
				Optional<Contract> firstContract = wperson.getFirstContractInMonth(year, competence.month);
				if (!firstContract.isPresent())
					continue;	//questo errore andrebbe segnalato, competenza senza che esista contratto

				IWrapperContract wcontract = wrapperFactory.create(firstContract.get());

				if (!wcontract.isDefined()) {
					continue;	//scarto la competence.
				}

			}

			//Filtro competenza non approvata
			if (competence.valueApproved == 0)
				continue;

			personSet.add(competence.person);

			//aggiungo la competenza alla mappa della persona
			Person person = competence.person;
			Map<CompetenceCode, Integer> personCompetences = mapPersonCompetenceRecap.get(person);

			if (personCompetences == null) 
				personCompetences = Maps.newHashMap();

			Integer value = personCompetences.get(competence.competenceCode);
			if (value != null) 
				value = value + competence.valueApproved;
			else
				value = competence.valueApproved;

			personCompetences.put(competence.competenceCode, value);

			mapPersonCompetenceRecap.put( person, personCompetences);

			//aggiungo la competenza al valore totale per la competenza
			value = totalValueAssigned.get(competence.competenceCode);

			if (value != null)
				value = value + competence.valueApproved;
			else
				value = competence.valueApproved;

			totalValueAssigned.put(competence.competenceCode, value);

		}

		List<IWrapperPerson> personList = FluentIterable
				.from(personSet)
				.transform(wrapperFunctionFactory.person()).toList();

		//FIXME inserisco il month per la navigazione delle tab competenze.
		// andrebbe un pò rifattorizzata questa parte...

		int month = LocalDate.now().getMonthOfYear();

		render(personList, totalValueAssigned, mapPersonCompetenceRecap, office, offices, year, month, onlyDefined);

	}
	
	
	public static void monthlyOvertime(Integer year, Integer month, String name, Integer page){
		
		if(!Security.getUser().get().person.isPersonInCharge)
			forbidden();
		
		User user = Security.getUser().get();
		if(page == null)
			page = 0;
		Table<Person, String, Integer> tableFeature = null;
		LocalDate beginMonth = null;
		if(year == 0 && month == 0){
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			beginMonth = new LocalDate(yearParams, monthParams, 1);
		}
		else{
			beginMonth = new LocalDate(year, month, 1);
		}
		CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
		SimpleResults<Person> simpleResults = personDao.listForCompetence(code, Optional.fromNullable(name), 
				Sets.newHashSet(user.person.office), 
				false, 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(),
				Optional.fromNullable(user.person));
		tableFeature = competenceManager.composeTableForOvertime(year, month, 
				page, name, user.person.office, beginMonth, simpleResults, code);
		

		if(year != 0 && month != 0)
			render(tableFeature, year, month, simpleResults, name);
		else{
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			render(tableFeature,yearParams,monthParams,simpleResults, name);
		}
	}

}
