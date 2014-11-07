package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import manager.ContractManager;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.ConfGeneral;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonWorkingTimeType;
import models.PersonYear;
import models.Qualification;
import models.Stamping;
import models.User;
import models.UsersRolesOffices;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.enumerate.ConfigurationFields;
import net.sf.oval.constraint.MinLength;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

import controllers.Resecure.NoCheck;
import dao.PersonDao;

@With( {Resecure.class, RequestInit.class} )
public class Persons extends Controller {

	
	
	
	public static final String USERNAME_SESSION_KEY = "username";

	@Inject
	static SecurityRules rules;

	@NoCheck
	public static void list(String name){

		rules.checkIfPermitted();

		LocalDate startEra = new LocalDate(1900,1,1);
		LocalDate endEra = new LocalDate(9999,1,1);
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), false, startEra, 
				endEra, false).list();

		render(personList);
	}

	@NoCheck
	public static void insertPerson() throws InstantiationException, IllegalAccessException {

		rules.checkIfPermitted();
		InitializationTime initializationTime = new InitializationTime();
		List<Office> officeList = Security.getOfficeAllowed();

		
		//Decisione: alla creazione come tipo orario viene assegnato Normale.
				
		render(initializationTime, officeList);

	}


	@NoCheck
	public static void save(Person person, Integer qualification, Integer office, Contract contract) {
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			Persons.list(null);
		}

		Office off = Office.findById(new Long(office));
		if(off == null) {

			flash.error("La sede selezionata non esiste. Effettuare una segnalazione.");
			Persons.list(null);
		}

		rules.checkIfPermitted(off);

		Logger.debug(person.name);
		
		/* creazione persona */

		Logger.debug("Saving person...");

		Qualification qual = Qualification.findById(new Long(qualification));
		person.qualification = qual;


		person.office = off;

		person.save();

		/* creazione utente */
		//User user = new User();
		//user.username = person.name.toLowerCase()+'.'+person.surname.toLowerCase(); 
		//Codec codec = new Codec();
		//user.password = codec.hexMD5("epas");
		//user.person = person;

		//user.save();
		//person.user = user;
		//person.save();

		/* creazione contratto */
		Logger.debug("Begin contract: %s", params.get("beginContract"));
		if(params.get("beginContract") == null){
			flash.error("Il contratto di %s %s deve avere una data di inizio. Utente cancellato. Reinserirlo con la data di inizio contratto valorizzata.", 
					person.name, person.surname);
			person.delete();
			render("@list");
		}
		LocalDate expireContract = null;
		LocalDate beginContract = new LocalDate(params.get("beginContract"));
		if(params.get("expireContract").equals("") || params.get("expireContract") == null)
			contract.expireContract = null;
		else			
			expireContract = new LocalDate(params.get("expireContract"));
		contract.beginContract = beginContract;
		contract.expireContract = expireContract;

		contract.person = person;

		if( params.get("onCertificate", Boolean.class) == null) 
			contract.onCertificate = false;
		else
			contract.onCertificate = params.get("onCertificate", Boolean.class);


		contract.save();
		contract.setVacationPeriods();
		

		//FIXME deve essere impostato in configurazione l'orario default
		WorkingTimeType wtt = WorkingTimeType.find("byDescription", "Normale").first();
		
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contract.beginContract;
		cwtt.endDate = contract.expireContract;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();
		
		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = contract.beginContract;
		csp.endTo = contract.expireContract;
		csp.fixedworkingtime = false;
		csp.save();
		contract.save();

		Long personId = person.id;
		List<String> usernameList = PersonUtility.composeUsername(person.name, person.surname);
		render("@insertUsername", personId, usernameList, person);


	}


	@NoCheck
	public static void insertUsername(Long personId){
		Person person = Person.findById(personId);
		if(person==null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}
		rules.checkIfPermitted(person.office);
		List<String> usernameList = new ArrayList<String>();
		usernameList = PersonUtility.composeUsername(person.name, person.surname);
		render(person, usernameList);
	}


	@NoCheck
	public static void updateUsername(Long personId, String username){

		Person person = Person.findById(personId);
		if(person==null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}
		rules.checkIfPermitted(person.office);

		if(person.user != null){
			person.user.save();
		}
		else{
			User user = new User();
			//user.id = person.id;
			Codec codec = new Codec();
			user.password = codec.hexMD5("epas");
			user.person = person;
			user.username = username;
			user.save();
			person.user = user;
			person.save();

		}			

		flash.success("%s %s inserito in anagrafica con il valore %s come username", person.name, person.surname, person.user.username);
		Persons.list(null);

	}

	@NoCheck
	public static void edit(Long personId){

		Person person = Person.findById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		LocalDate date = new LocalDate();
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract", person).fetch();
		List<Office> officeList = Security.getOfficeAllowed();	
		List<ContractStampProfile> contractStampProfileList = ContractStampProfile.find("Select csp from ContractStampProfile csp, Contract c "
				+ "where csp.contract = c and c.person = ? order by csp.startFrom", person).fetch();
		
		InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ?", person).first();
		Integer month = date.getMonthOfYear();
		Integer year = date.getYear();
		LocalDate actualDate = new LocalDate();
		Long id = person.id;		
		render(person, contractList, contractStampProfileList, initTime, month, year, id, actualDate,officeList);
	}

	public static void update(Person person, Office office, Integer qualification){

		if(person==null) {

			flash.error("La persona da modificare non esiste. Operazione annullata");
			Persons.list(null);
		}
		rules.checkIfPermitted(person.office);
		rules.checkIfPermitted(office);

		if(office!=null) {
			person.office = office;
		}

		Qualification q = Qualification.find("Select q from Qualification q where q.qualification = ?", qualification).first();
		if(q != null) {
			person.qualification = q;
		}

		person.save();
		flash.success("Modificate informazioni per l'utente %s %s", person.name, person.surname);

		Persons.edit(person.id);	
	}

	public static void deletePerson(Long personId){
		Person person = Person.findById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		render(person);
	}

	public static void deletePersonConfirmed(Long personId){
		Person person = Person.findById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		/***** person.delete(); ******/
		
		if(person.user != null) {

			if(person.user.usersRolesOffices.size() > 0) {
				flash.error("Impossibile eliminare una persona che detiene diritti di amministrazione su almeno una sede. Rimuovere tali diritti e riprovare.");
				Persons.list(null);
			}
			
			if(person.user.username.equals(Security.getUser().get().username)) {
				
				flash.error("Impossibile eliminare la persona loggata nella sessione corrente. Operazione annullata.");
				Persons.list(null);
			}
		}
		
		
		
		
		String name = person.name;
		String surname = person.surname;


		Logger.debug("Elimino competenze...");
		JPAPlugin.startTx(false);

		// Eliminazione competenze
		for(Competence c : person.competences){
			long id = c.id;
			c = Competence.findById(id);
			c.delete();
		}
		JPAPlugin.closeTx(false);
		
		// Eliminazione contratti
		Logger.debug("Elimino contratti...");
		JPAPlugin.startTx(false);
		List<Contract> helpList = Contract.find("Select c from Contract c where c.person = ?", person).fetch();
		
		
		for(Contract c : helpList){			

			Logger.debug("Elimino contratto di %s %s che va da %s a %s", person.name, person.surname, c.beginContract, c.expireContract);
						
			// Eliminazione orari di lavoro 
			List<ContractWorkingTimeType> cwttList = ContractWorkingTimeType.find("Select cwtt from ContractWorkingTimeType cwtt where cwtt.contract = ?"
					, c).fetch();
			for(ContractWorkingTimeType cwtt : cwttList){
				cwtt.delete();
			}
			// Eliminazione stamp profile
			List<ContractStampProfile> cspList = ContractStampProfile.find("Select csp from ContractStampProfile csp where csp.contract = ?"
					, c).fetch();
			for(ContractStampProfile csp : cspList){
				csp.delete();
			}

			
			c.delete();
			person = Person.findById(personId);
			person.contracts.remove(c);
			
			person.save();

		}
		
		// Eliminazione assenze da inizializzazione
		for(InitializationAbsence ia : person.initializationAbsences){
			long id = ia.id;
			ia = InitializationAbsence.findById(id);
			ia.delete();
		}

		// Eliminazione tempi da inizializzazione
		for(InitializationTime ia : person.initializationTimes){
			long id = ia.id;
			ia = InitializationTime.findById(id);
			ia.delete();
		}
		



		JPAPlugin.closeTx(false);
		Logger.debug("Elimino timbrature e dati mensili e annuali...");
		JPAPlugin.startTx(false);
		person = Person.findById(personId);
		// Eliminazione figli in anagrafica
		for(PersonChildren pc : person.personChildren){
			long id = pc.id;
			Logger.debug("Elimino figli...");
			pc = PersonChildren.findById(id);
			pc.delete();
		}

		// Eliminazione person day
		List<PersonDay> helpPdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ?", person).fetch();
		for(PersonDay pd : helpPdList){

			pd.delete();
			person.personDays.remove(pd);
			person.save();
		}

		// Eliminazione ore di formazione
		if(person.personHourForOvertime != null)
			person.personHourForOvertime.delete();

		// Eliminazione turni
		if(person.personShift != null)
			person.personShift.delete();
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		person = Person.findById(personId);
		//Eliminazione riepiloghi annuali
		for(PersonYear py : person.personYears){
			py.delete();
		}
		// Eliminazione reperibilità
		if(person.reperibility != null)
			person.reperibility.delete();
		// Eliminazione stamp profile associati alla persona
//		for(StampProfile sp : person.stampProfiles){
//			long id = sp.id;
//			sp = StampProfile.findById(id);
//			sp.delete();
//		}

		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		person = Person.findById(personId);
		//Eliminazione competenze valide
		
		JPAPlugin.closeTx(false);
		
//		//Eliminazione user
//		JPAPlugin.startTx(false);
//		person = Person.findById(personId);
//		if(person.user != null) {
//			User user = User.findById(person.user.id);
//			user.delete();
//
//		}
//		JPAPlugin.closeTx(false);

		// Eliminazione persona e user
		JPAPlugin.startTx(false);
		person = Person.findById(personId);
		Long userId = null;
		if(person.user != null) {
			userId = person.user.id;
		}
		person.delete();
		JPAPlugin.closeTx(false);
		
		JPAPlugin.startTx(false);
		if(userId != null) {
			User user = User.findById(userId);
			user.delete();
		}
		JPAPlugin.closeTx(false);

		flash.success("La persona %s %s eliminata dall'anagrafica insieme a tutti i suoi dati.",name, surname);

		Persons.list(null);

	}

	public static void showCurrentVacation(Long personId){

		Person person = Person.findById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		VacationPeriod vp = person.getCurrentContract().getCurrentVacationPeriod();
		render(person, vp);
	}

	public static void showCurrentContractWorkingTimeType(Long personId) {

		Person person = Person.findById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		Contract currentContract = person.getCurrentContract();
		if(currentContract == null) {

			flash.error("La persona selezionata non ha contratto attivo, operazione annullata.");
			Persons.list(null);
		}

		ContractWorkingTimeType cwtt = currentContract.getContractWorkingTimeType(LocalDate.now());
		WorkingTimeType wtt = cwtt.workingTimeType;
		render(person, cwtt, wtt);
	}

	public static void insertContract(Person person){
		if(person == null) {

			flash.error("Persona inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		Contract con = new Contract();
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		render(con, person, wttList);
	}

	public static void saveContract(@Required LocalDate dataInizio, @Valid LocalDate dataFine, Person person, WorkingTimeType wtt, boolean onCertificate){

		//Controllo parametri
		if(person==null) {

			flash.error("Persona inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		if(dataInizio==null) {

			flash.error("Errore nel fornire il parametro data inizio contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(person.id);
		}
		if(validation.hasErrors()) {

			flash.error("Errore nel fornire il parametro data fine contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(person.id);
		}

		//Tipo orario
		if(wtt == null) {
			flash.error("Errore nel fornire il parametro tipo orario. Operazione annullata.");
			Persons.edit(person.id);
		}

		//Creazione nuovo contratto
		Contract contract = new Contract();
		contract.beginContract = dataInizio;
		contract.expireContract = dataFine;
		contract.onCertificate = onCertificate;
		contract.person = person;

		//Date non si sovrappongono con gli altri contratti della persona	
		if( !contract.isProperContract() ) {

			flash.error("Il nuovo contratto si interseca con contratti precedenti. Controllare le date di inizio e fine. Operazione annulalta.");
			Persons.edit(person.id);
		}

		contract.save();
		contract.setVacationPeriods();
		contract.save();
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = dataInizio;
		cwtt.endDate = dataFine;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();
		contract.save();
		
		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = dataInizio;
		csp.endTo = dataFine;
		csp.fixedworkingtime = false;
		csp.save();
		contract.save();
		
		flash.success("Il contratto per %s %s è stato correttamente salvato", person.name, person.surname);

		Persons.edit(person.id);
	}

	public static void modifyContract(Long contractId){
		Contract contract = Contract.findById(contractId);
		if(contract == null)
		{
			flash.error("Non è stato trovato nessun contratto con id %s per il dipendente ", contractId);
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		render(contract);
	}

	public static void updateContract(Contract contract, @Required LocalDate begin, @Valid LocalDate expire, @Valid LocalDate end, boolean onCertificate){

		//Controllo dei parametri
		if(contract == null) {

			flash.error("Contratto inesistente, operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		if(begin==null){

			flash.error("Errore nel fornire il parametro data inizio contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.person.id);
		}
		if(validation.hasError("expire")) {

			flash.error("Errore nel fornire il parametro data fine contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.person.id);
		}
		if(validation.hasError("end")) {

			flash.error("Errore nel fornire il parametro data terminazione contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.person.id);
		}

		contract.beginContract = begin;
		contract.expireContract = expire;
		contract.endContract = end;

		//Date non si sovrappongono con gli altri contratti della persona	
		if( !contract.isProperContract() ) {

			flash.error("Il contratto si interseca con altri contratti della persona. Controllare le date di inizio e fine. Operazione annulalta.");
			Persons.edit(contract.person.id);
		}

		contract.onCertificate = onCertificate;
		contract.setVacationPeriods();
		contract.updateContractWorkingTimeType();
		contract.updateContractStampProfile();
		
		//Ricalcolo valori
		DateInterval contractDateInterval = contract.getContractDateInterval();
		ContractManager.recomputeContract(contract, contractDateInterval.getBegin(), contractDateInterval.getEnd());
	
		contract.save();

		flash.success("Aggiornato contratto per il dipendente %s %s", contract.person.name, contract.person.surname);

		Persons.edit(contract.person.id);

	}

	public static void deleteContract(Long contractId){

		Contract contract = Contract.findById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		render(contract);
	}

	public static void deleteContractConfirmed(Long contractId){

		Contract contract = Contract.findById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		contract.delete();

		flash.error("Contratto eliminato con successo.");
		Persons.edit(contract.person.id);
	}

	public static void updateSourceContract(Long contractId){

		Contract contract = Contract.findById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		LocalDate initUse = new LocalDate(
				ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, 
						Security.getUser().get().person.office));
		render(contract, initUse);
	}


	public static void saveSourceContract(Contract contract) {

		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		if(contract.sourceVacationLastYearUsed==null) contract.sourceVacationLastYearUsed=0;
		if(contract.sourceVacationCurrentYearUsed==null) contract.sourceVacationCurrentYearUsed=0;
		if(contract.sourcePermissionUsed==null) contract.sourcePermissionUsed=0;
		if(contract.sourceRemainingMinutesCurrentYear==null) contract.sourceRemainingMinutesCurrentYear=0;
		if(contract.sourceRemainingMinutesLastYear==null) contract.sourceRemainingMinutesLastYear=0;
		if(contract.sourceRecoveryDayUsed==null) contract.sourceRecoveryDayUsed=0;

		contract.save();

		//Ricalcolo valori
		DateInterval contractDateInterval = contract.getContractDateInterval();
		ContractManager.recomputeContract(contract, contractDateInterval.getBegin(), contractDateInterval.getEnd());
		//contract.buildContractYearRecap();

		flash.success("Dati di inizializzazione definiti con successo ed effettuati i ricalcoli.");

		Persons.edit(contract.person.id);

	}


	public static void updateContractWorkingTimeType(Long id)
	{

		Contract contract = Contract.findById(id);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);


		//La lista dei tipi orario ammessi per la persona
		List<WorkingTimeType> wttDefault = WorkingTimeType.getDefaultWorkingTimeTypes();
		List<WorkingTimeType> wttAllowed = contract.person.office.getEnabledWorkingTimeType(); 
		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		wttList.addAll(wttDefault);
		wttList.addAll(wttAllowed);
		

		render(contract, wttList);
	}

	public static void splitContractWorkingTimeType(ContractWorkingTimeType cwtt, LocalDate splitDate)
	{
		//Controllo integrità richiesta
		if(cwtt==null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(cwtt.contract.person.office);

		if(validation.hasError("splitDate")) {

			flash.error("Errore nel fornire il parametro data. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(cwtt.contract.person.id);
		}

		if(!DateUtility.isDateIntoInterval(splitDate, new DateInterval(cwtt.beginDate, cwtt.endDate))) {

			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			Persons.edit(cwtt.contract.person.id);
		}

		DateInterval first = new DateInterval(cwtt.beginDate, splitDate.minusDays(1));
		if(! DateUtility.isIntervalIntoAnother(first, cwtt.contract.getContractDateInterval())) {
			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			Persons.edit(cwtt.contract.person.id);
		}

		//agire
		ContractWorkingTimeType cwtt2 = new ContractWorkingTimeType();
		cwtt2.contract = cwtt.contract;
		cwtt2.beginDate = splitDate;
		cwtt2.endDate = cwtt.endDate;
		cwtt2.workingTimeType = cwtt.workingTimeType;
		cwtt2.save();

		cwtt.endDate = splitDate.minusDays(1);
		cwtt.save();
		flash.success("Orario di lavoro correttamente suddiviso in due sottoperiodi con tipo orario %s.", cwtt.workingTimeType.description);
		Persons.edit(cwtt.contract.person.id);	
	}

	public static void deleteContractWorkingTimeType(ContractWorkingTimeType cwtt)
	{
		if(cwtt==null){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}	

		rules.checkIfPermitted(cwtt.contract.person.office);

		Contract contract = cwtt.contract;
		//List<ContractWorkingTimeType> cwttList = Lists.newArrayList(contract.contractWorkingTimeType);

		int index = contract.getContractWorkingTimeTypeAsList().indexOf(cwtt);
		if(contract.getContractWorkingTimeTypeAsList().size()<index){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Persons.edit(cwtt.contract.person.id);	
		}

		ContractWorkingTimeType previous = contract.getContractWorkingTimeTypeAsList().get(index-1);
		previous.endDate = cwtt.endDate;
		previous.save();
		
		//Safe remove from hibernate set of elements (pattern da riutilizzare quando serve)
		for (Iterator<ContractWorkingTimeType> i = contract.contractWorkingTimeType.iterator(); i.hasNext();) {
		    ContractWorkingTimeType cwttt = i.next();
		    if(cwttt.id.equals(cwtt.id)) {
				i.remove();
			}
		}

		cwtt.delete();
		
		//Ricalcolo valori
		ContractManager.recomputeContract(cwtt.contract, cwtt.beginDate, null);
		
		flash.success("Orario di lavoro eliminato correttamente. Attribuito al periodo eliminato il tipo orario %s.", previous.workingTimeType.description);
		Persons.edit(cwtt.contract.person.id);	
	}

	public static void changeTypeOfContractWorkingTimeType(ContractWorkingTimeType cwtt, WorkingTimeType newWtt)
	{
		if(cwtt==null || newWtt==null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(cwtt.contract.person.office);
		rules.checkIfPermitted(newWtt.office);

		cwtt.workingTimeType = newWtt;
		cwtt.save();

		//Ricalcolo valori
		ContractManager.recomputeContract(cwtt.contract, cwtt.beginDate, null);

		flash.success("Cambiato correttamente tipo orario per il periodo a %s.", cwtt.workingTimeType.description);
		Persons.edit(cwtt.contract.person.id);
	}

	public static void changePassword(){
		User user = Security.getUser().get();
		notFoundIfNull(user);
		render(user);
	}

	public static void savePassword(@Required String vecchiaPassword, 
			@MinLength(5) @Required String nuovaPassword, @MinLength(5) @Required String confermaPassword){

		User user = User.find("SELECT u FROM User u where username = ? and password = ?", 
				Security.getUser().get().username, Hashing.md5().hashString(vecchiaPassword,  Charsets.UTF_8).toString()).first();
		if(user == null) {
			flash.error("Nessuna corrispondenza trovata fra utente e vecchia password inserita.");
			Persons.changePassword();
		}

		if(validation.hasErrors() || !nuovaPassword.equals(confermaPassword)) {
			flash.error("Tutti i campi devono essere valorizzati. "
					+ "La passord deve essere almeno lunga 5 caratteri. Operazione annullata.");
			Persons.changePassword();
		}

		notFoundIfNull(user);

		Codec codec = new Codec();

		user.password = codec.hexMD5(nuovaPassword);
		user.save();
		flash.success(Messages.get("passwordSuccessfullyChanged"));
		Persons.changePassword();
	}
	
	public static void resetPassword(@MinLength(5) @Required String nuovaPassword, @MinLength(5) @Required String confermaPassword) throws Throwable {
		
		User user = Security.getUser().get();
		if(user.expireRecoveryToken == null || !user.expireRecoveryToken.equals(LocalDate.now()))
		{
			flash.error("La procedura di recovery password è scaduta. Operazione annullata.");
			Secure.login();
		}
		
		if(validation.hasErrors() || !nuovaPassword.equals(confermaPassword)) {
			flash.error("Tutti i campi devono essere valorizzati. "
					+ "La passord deve essere almeno lunga 5 caratteri. Operazione annullata.");
			LostPassword.lostPasswordRecovery(user.recoveryToken);
		}
		
		Codec codec = new Codec();
		user.password = codec.hexMD5(nuovaPassword);
		user.recoveryToken = null;
		user.expireRecoveryToken = null;
		user.save();
		
		flash.success("La password è stata resettata con successo.");
		Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
	}

	@NoCheck
	public static void insertChild(Long personId){

		Person person = Person.findById(personId);
		PersonChildren personChildren = new PersonChildren();
		render(person, personChildren);
	}

	public static void saveChild(){

		PersonChildren personChildren = new PersonChildren();
		Person person = Person.findById(params.get("personId", Long.class));
		rules.checkIfPermitted(person.office);
		personChildren.name = params.get("name");
		personChildren.surname = params.get("surname");
		personChildren.bornDate = new LocalDate(params.get("bornDate"));
		personChildren.person = person;
		personChildren.save();
		person.save();
		flash.success("Aggiunto %s %s nell'anagrafica dei figli di %s %s", personChildren.name, personChildren.surname, person.name, person.surname);
		Persons.insertChild(person.id);
	}

	public static void personChildrenList(Long personId){

		Person person = Person.findById(personId);
		rules.checkIfPermitted(person.office);
		render(person);
	}




	public static void updateContractStampProfile(Long id){
		ContractStampProfile contract = ContractStampProfile.findById(id);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}
		
		rules.checkIfPermitted(contract.contract.person.office);
		
		List<String> listTipo = Lists.newArrayList();
		
		for(int i = 1; i <=2; i++){
			String t = null;
			if(i % 2 == 0)
				t = new String("Timbratura automatica");
			else
				t = new String("Timbratura manuale");
			listTipo.add(t);
			
		}
		render(contract, listTipo);
	}
	
	public static void changeTypeOfContractStampProfile(ContractStampProfile contract, String newtipo){
		if(contract==null || newtipo==null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}
				
		rules.checkIfPermitted(contract.contract.person.office);
		if(newtipo.equals("Timbratura automatica"))
			contract.fixedworkingtime = true;
		else
			contract.fixedworkingtime = false;
		
		contract.save();
		
		ContractManager.recomputeContract(contract.contract, contract.startFrom, null);
		
		flash.success("Cambiata correttamente tipologia di timbratura per il periodo a %s.", newtipo);
		Persons.edit(contract.contract.person.id);
		
	}

	public static void splitContractStampProfile(ContractStampProfile contract, LocalDate splitDate){
		if(contract==null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(contract.contract.person.office);
		

		if(validation.hasError("splitDate")) {

			flash.error("Errore nel fornire il parametro data. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.contract.person.id);
		}
		
		if(!DateUtility.isDateIntoInterval(splitDate, new DateInterval(contract.startFrom, contract.endTo))) {

			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			Persons.edit(contract.contract.person.id);
		}
		DateInterval first = new DateInterval(contract.startFrom, splitDate.minusDays(1));
		if(! DateUtility.isIntervalIntoAnother(first, contract.contract.getContractDateInterval())) {
			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			Persons.edit(contract.contract.person.id);
		}
		
		ContractStampProfile csp2 = new ContractStampProfile();
		csp2.contract = contract.contract;
		csp2.startFrom = splitDate;
		csp2.endTo = contract.endTo;
		csp2.fixedworkingtime = contract.fixedworkingtime;
		csp2.save();

		contract.endTo = splitDate.minusDays(1);
		contract.save();
		flash.success("Tipo timbratura suddivisa in due sottoperiodi con valore %s.", contract.fixedworkingtime);
		Persons.edit(contract.contract.person.id);	
		
	}
	
	public static void deleteContractStampProfile(Long contractStampProfileId){
		ContractStampProfile csp = ContractStampProfile.findById(contractStampProfileId);
		if(csp==null){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}	

		rules.checkIfPermitted(csp.contract.person.office);

		Contract contract = csp.contract;
		//List<ContractWorkingTimeType> cwttList = Lists.newArrayList(contract.contractWorkingTimeType);

		int index = contract.getContractStampProfileAsList().indexOf(csp);
		if(contract.getContractStampProfileAsList().size()<index){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Persons.edit(csp.contract.person.id);	
		}

		ContractStampProfile previous = contract.getContractStampProfileAsList().get(index-1);
		previous.endTo = csp.endTo;
		previous.save();
		csp.delete();
		
		//Ricalcolo i valori
		//FIXME, trovare un modo per pulire lo heap che ci trovo sempre quello eliminato	
		for(ContractStampProfile cspp : contract.contractStampProfile){
			if(cspp.id.equals(csp.id)) {
				contract.contractStampProfile.remove(cspp);
			}
		}
		
		//Ricalcolo i valori
		ContractManager.recomputeContract(previous.contract, csp.startFrom, null);
		
		flash.success("Tipologia di timbratura eliminata correttamente. Tornati alla precedente che ha timbratura automatica con valore: %s", previous.fixedworkingtime);
		Persons.edit(csp.contract.person.id);
	}
	
	
	public static void modifySendEmail(Long personId){
		
		Person person = Person.findById(personId);
		rules.checkIfPermitted(person.office);
		render(person);
	}
	
	public static void updateSendEmail(Person person, boolean wantEmail){
		if(person == null) {

			flash.error("Persona inesistente, operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);
		person.wantEmail = wantEmail;
		person.save();
		flash.success("Cambiata gestione di invio mail al dipendente %s %s", person.name, person.surname);
		Persons.edit(person.id);
	}
}
