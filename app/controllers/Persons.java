package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.CompetenceManager;
import manager.ConfGeneralManager;
import manager.ContractManager;
import manager.ContractStampProfileManager;
import manager.ContractWorkingTimeTypeManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.Qualification;
import models.User;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.enumerate.Parameter;
import net.sf.oval.constraint.MinLength;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.gdata.util.common.base.Preconditions;

import controllers.Resecure.NoCheck;
import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import exceptions.EpasExceptionNoSourceData;

@With( {Resecure.class, RequestInit.class} )
public class Persons extends Controller {

	public static final String USERNAME_SESSION_KEY = "username";

	@Inject
	static SecurityRules rules;
	
	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory; 
	
	@Inject
	static IWrapperFactory wrapperFactory;
	
	@Inject
	static PersonDao personDao;
	
	@Inject
	static AbsenceDao absenceDao;
	
	@Inject
	static OfficeDao officeDao;

	@Inject
	static ContractManager contractManager;
	
	@Inject 
	static PersonDayManager personDayManager;
	
	@Inject 
	static CompetenceDao competenceDao;
	
	@Inject 
	static CompetenceManager competenceManager;

	private final static Logger log = LoggerFactory.getLogger(Persons.class);
		
	public static void list(String name){

		LocalDate startEra = new LocalDate(1900,1,1);
		LocalDate endEra = new LocalDate(9999,1,1);
		List<Person> simplePersonList = PersonDao.list(Optional.fromNullable(name),
				officeDao.getOfficeAllowed(Security.getUser().get()), false, startEra,
				endEra, false).list();
		
		List<IWrapperPerson> personList = FluentIterable
				.from(simplePersonList)
				.transform(wrapperFunctionFactory.person()).toList();
		render(personList);
	}

	public static void insertPerson() {

		Person person = new Person();
		Contract contract = new Contract();

		render(person, contract);

	}

	public static void save(@Valid @Required Person person,
			@Valid @Required Qualification qualification, @Valid @Required Office office,
			@Valid @Required Contract contract) {

		if(Validation.hasErrors()) {
			
			flash.error("Inserire correttamente tutti i parametri");
			params.flash(); // add http parameters to the flash scope
			render("@insertPerson", person, qualification, office);
		}
		
		rules.checkIfPermitted(office);
		
		person.qualification = qualification;
		person.office = office;

		contract.person = person;
		
		if(!ContractManager.contractCrossFieldValidation(contract)){
			
			flash.error("Errore nella validazione del contratto. Inserire correttamente tutti i parametri.");
			params.flash(); // add http parameters to the flash scope
			render("@insertPerson", person, qualification, office);
		}
		
		person.save();

		contract.save();

		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeByDescription("Normale");
		ContractManager.properContractCreate(contract, wtt);

		Long personId = person.id;

		Persons.insertUsername(personId);

	}


	public static void insertUsername(Long personId){
		Person person = PersonDao.getPersonById(personId);
		if(person==null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		List<String> usernameList = PersonUtility.composeUsername(person.name, person.surname);

		render(person, usernameList);
	}


	public static void updateUsername(Long personId, String username){

		Person person = PersonDao.getPersonById(personId);
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
			user.password = Codec.hexMD5("epas");
			user.person = person;
			user.username = username;
			user.save();
			person.user = user;
			person.save();

		}

		flash.success("%s %s inserito in anagrafica con il valore %s come username", person.name, person.surname, person.user.username);
		Persons.list(null);

	}

	public static void edit(Long personId){

		Person person = PersonDao.getPersonById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);


		List<Contract> contractList = ContractDao.getPersonContractList(person);
		Set<Office> officeList = officeDao.getOfficeAllowed(Security.getUser().get());

		List<ContractStampProfile> contractStampProfileList =
				ContractDao.getPersonContractStampProfile(Optional.fromNullable(person), Optional.<Contract>absent());

		LocalDate actualDate = new LocalDate();
		Integer month = actualDate.getMonthOfYear();
		Integer year = actualDate.getYear();

		Long id = person.id;
		render(person, contractList, contractStampProfileList, month, year, id, actualDate, officeList);
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

		Optional<Qualification> q = QualificationDao.byQualification(qualification);
		if( !q.isPresent() ) {
			flash.error("La qualifica selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		person.qualification = q.get();
		person.save();
		flash.success("Modificate informazioni per l'utente %s %s", person.name, person.surname);

		Persons.edit(person.id);
	}

	public static void deletePerson(Long personId){
		Person person = PersonDao.getPersonById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		render(person);
	}

	public static void deletePersonConfirmed(Long personId){
		Person person = PersonDao.getPersonById(personId);
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

		log.debug("Elimino competenze...");
		JPAPlugin.startTx(false);

		// Eliminazione competenze
		competenceManager.deletePersonCompetence(person);
		JPAPlugin.closeTx(false);

		// Eliminazione contratti
		log.debug("Elimino contratti...");
		JPAPlugin.startTx(false);
		ContractManager.deletePersonContracts(person);

		// Eliminazione assenze e tempi da inizializzazione
		ContractManager.deleteInitializations(person);

		JPAPlugin.closeTx(false);
		log.debug("Elimino timbrature e dati mensili e annuali...");
		JPAPlugin.startTx(false);
		person = PersonDao.getPersonById(personId);
		// Eliminazione figli in anagrafica
		PersonManager.deletePersonChildren(person);

		// Eliminazione person day
		personDayManager.deletePersonDays(person);
	
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		person = PersonDao.getPersonById(personId);
		//Eliminazione riepiloghi annuali
		
		// Eliminazione reperibilità turni e ore di formazione e riepiloghi annuali
		PersonManager.deleteShiftReperibilityTrainingHoursAndYearRecap(person);

		JPAPlugin.closeTx(false);

		// Eliminazione persona e user
		JPAPlugin.startTx(false);
		person = PersonDao.getPersonById(personId);
		
		Long userId = null;
		if(person.user != null) {
			userId = person.user.id;
		}
		person.delete();
		JPAPlugin.closeTx(false);

		JPAPlugin.startTx(false);
		if(userId != null) {
			User user = UserDao.getUserById(userId, Optional.<String>absent());
			user.delete();
		}
		JPAPlugin.closeTx(false);

		flash.success("La persona %s %s eliminata dall'anagrafica insieme a tutti i suoi dati.",name, surname);

		Persons.list(null);

	}

	public static void showCurrentVacation(Long personId){

		Person person = PersonDao.getPersonById(personId);
		if(person == null) {
			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);

		IWrapperPerson wPerson = wrapperFactory.create(person);
		
		Preconditions.checkState(wPerson.getCurrentVacationPeriod().isPresent());
		
		VacationPeriod vp = wPerson.getCurrentVacationPeriod().get();
		render(person, vp);
	}

	public static void showCurrentContractWorkingTimeType(Long personId) {

		Person person = PersonDao.getPersonById(personId);

		if(person == null) {
			flash.error("La persona selezionata non esiste. Operazione annullata");
			Persons.list(null);
		}

		rules.checkIfPermitted(person.office);
		
		IWrapperPerson wPerson = wrapperFactory.create(person);
		
		Preconditions.checkState(wPerson.getCurrentContractWorkingTimeType().isPresent());

		ContractWorkingTimeType cwtt = wPerson.getCurrentContractWorkingTimeType().get();
		
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
		List<WorkingTimeType> wttList = WorkingTimeTypeDao.getAllWorkingTimeType();
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
		if(Validation.hasErrors()) {

			flash.error("Errore nel fornire il parametro data fine contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(person.id);
		}

		//Tipo orario
		if(wtt == null) {
			flash.error("Errore nel fornire il parametro tipo orario. Operazione annullata.");
			Persons.edit(person.id);
		}

		//Creazione nuovo contratto
		if(!ContractManager.saveContract(dataInizio, dataFine, onCertificate, person, wtt).equals("")){
			flash.error(ContractManager.saveContract(dataInizio, dataFine, onCertificate, person, wtt));
			Persons.edit(person.id);
			
		}	

		flash.success("Il contratto per %s %s è stato correttamente salvato", person.name, person.surname);

		Persons.edit(person.id);
	}

	public static void modifyContract(Long contractId){
		Contract contract = ContractDao.getContractById(contractId);
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
		if( ! ContractManager.isProperContract(contract) ) {

			flash.error("Il contratto si interseca con altri contratti della persona. Controllare le date di inizio e fine. Operazione annulalta.");
			Persons.edit(contract.person.id);
		}

		contract.onCertificate = onCertificate;

		ContractManager.properContractUpdate(contract);

		//Ricalcolo valori
		DateInterval contractDateInterval = contract.getContractDateInterval();
		
		try {
			contractManager.recomputeContract(contract, contractDateInterval.getBegin(), contractDateInterval.getEnd());

			contract.save();

			flash.success("Aggiornato contratto per il dipendente %s %s", contract.person.name, contract.person.surname);

		} catch(EpasExceptionNoSourceData e) { 
			flash.error("Mancano i dati di inizializzazione per " 
    				+ contract.person.fullName());
		}
		
		Persons.edit(contract.person.id);

	}

	public static void deleteContract(Long contractId){

		Contract contract = ContractDao.getContractById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		render(contract);
	}

	public static void deleteContractConfirmed(Long contractId){

		Contract contract = ContractDao.getContractById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		for(ContractStampProfile csp : contract.contractStampProfile){
			csp.delete();
		}

		contract.delete();

		flash.error("Contratto eliminato con successo.");
		Persons.edit(contract.person.id);
	}

	public static void updateSourceContract(Long contractId){

		Contract contract = ContractDao.getContractById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		LocalDate initUse = ConfGeneralManager.getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, 
				Security.getUser().get().person.office);
				
		render(contract, initUse);
	}


	public static void saveSourceContract(Contract contract) {

		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		ContractManager.saveSourceContract(contract);

		//Ricalcolo valori
		try {
			DateInterval contractDateInterval = contract.getContractDateInterval();
			
			contractManager.recomputeContract(contract, contractDateInterval.getBegin(), contractDateInterval.getEnd());
			
			flash.success("Dati di inizializzazione definiti con successo ed effettuati i ricalcoli.");

		} catch(EpasExceptionNoSourceData e) { 
			flash.error("Mancano i dati di inizializzazione per " 
    				+ contract.person.fullName());
		}
		
		Persons.edit(contract.person.id);

	}


	public static void updateContractWorkingTimeType(Long id)
	{

		Contract contract = ContractDao.getContractById(id);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		//La lista dei tipi orario ammessi per la persona
		List<WorkingTimeType> wttDefault = WorkingTimeTypeDao.getDefaultWorkingTimeType();
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
		ContractWorkingTimeTypeManager.saveSplitContractWorkingTimeType(cwtt, splitDate);
		
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

		int index = contract.getContractWorkingTimeTypeAsList().indexOf(cwtt);
		if(contract.getContractWorkingTimeTypeAsList().size()<index){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Persons.edit(cwtt.contract.person.id);
		}
		ContractWorkingTimeType previous = contract.getContractWorkingTimeTypeAsList().get(index-1);
		ContractWorkingTimeTypeManager.deleteContractWorkingTimeType(contract, index, cwtt);

		//Ricalcolo valori
		try {
			contractManager.recomputeContract(cwtt.contract, cwtt.beginDate, null);

			flash.success("Orario di lavoro eliminato correttamente. Attribuito al periodo eliminato il tipo orario %s.", previous.workingTimeType.description);
			
		} catch(EpasExceptionNoSourceData e) { 
			flash.error("Mancano i dati di inizializzazione per " 
    				+ cwtt.contract.person.fullName());
		}
		
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
		try {
			contractManager.recomputeContract(cwtt.contract, cwtt.beginDate, null);

			flash.success("Cambiato correttamente tipo orario per il periodo a %s.", cwtt.workingTimeType.description);

		} catch(EpasExceptionNoSourceData e) { 
			flash.error("Mancano i dati di inizializzazione per " 
    				+ cwtt.contract.person.fullName());
		}
		Persons.edit(cwtt.contract.person.id);
	}

	public static void changePassword(){
		User user = Security.getUser().get();
		notFoundIfNull(user);
		render(user);
	}

	public static void savePassword(@Required String vecchiaPassword,
			@MinLength(5) @Required String nuovaPassword, @MinLength(5) @Required String confermaPassword){

		User user = UserDao.getUserByUsernameAndPassword(Security.getUser().get().username, Optional.fromNullable(Hashing.md5().hashString(vecchiaPassword,  Charsets.UTF_8).toString()));

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

	public static void childrenList(Long personId){

		Person person = PersonDao.getPersonById(personId);
		render(person);
	}
	
	public static void insertChild(Long personId){
		
		Person person = PersonDao.getPersonById(personId);
		notFoundIfNull(person);
		rules.checkIfPermitted(person.office);
		render(person);
	}
	
	public static void editChild(Long childId){
		
		PersonChildren child = PersonChildrenDao.getById(childId);
		notFoundIfNull(child);
		Person person = child.person;
	
		render("@insertChild", child, person);
	}
	
	public static void removeChild(Long childId){
		
		PersonChildren child = PersonChildrenDao.getById(childId);
		notFoundIfNull(child);
		Person person = child.person;
	
		render(child, person);
	}
	
	public static void deleteChild(Long childId){
		
		PersonChildren child = PersonChildrenDao.getById(childId);
		notFoundIfNull(child);
		Person person = child.person;
		rules.checkIfPermitted(person.office);
		
		flash.error("Eliminato %s %s dall'anagrafica dei figli di %s", child.name, child.surname, person.getFullname());
		child.delete();
	
		childrenList(person.id);
	}


	public static void saveChild(@Valid PersonChildren child,Person person){
		
		Preconditions.checkState(person.isPersistent());
		
		if(Validation.hasErrors()) {
			render("@insertChild", person, child);
		}
		
//		Controlli nel caso di un nuovo inserimento
		if(!child.isPersistent()){
			for(PersonChildren p : PersonChildrenDao.getAllPersonChildren(person)){
				
				if (p.name.equals(child.name) && p.surname.equals(child.surname) || 
						p.name.equals(child.surname) && p.surname.equals(child.name)){
					flash.error("%s %s già presente in anagrafica", child.name, child.surname);
					render("@insertChild", person, child);
				}
				if(p.bornDate.isBefore(child.bornDate.plusMonths(9)) || p.bornDate.isBefore(child.bornDate.minusMonths(9))){
					flash.error("Attenzione: la data di nascita inserita risulta troppo vicina alla data di nascita di un'altro figlio. Verificare!", child.bornDate);
				}	
			}
		}
		
		rules.checkIfPermitted(person.office);
		
		child.person = person;
		child.save();
		
		log.info("Aggiunto/Modificato {} {} nell'anagrafica dei figli di {}",
				new Object[]{child.name, child.surname, person});
		flash.success("Salvato figlio nell'anagrafica dei figli di %s", person.getFullname());

		childrenList(person.id);
	}

	public static void updateContractStampProfile(Long id){

		ContractStampProfile contract = ContractDao.getContractStampProfileById(id);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}

		rules.checkIfPermitted(contract.contract.person.office);

		List<String> listTipo = Lists.newArrayList();

		listTipo.add("Timbratura automatica");
		listTipo.add("Timbratura manuale");

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

		try {
			contractManager.recomputeContract(contract.contract, contract.startFrom, null);

			flash.success("Cambiata correttamente tipologia di timbratura per il periodo a %s.", newtipo);
			
		} catch(EpasExceptionNoSourceData e) {
			flash.error("Mancano i dati di inizializzazione per " 
					+ contract.contract.person.fullName());
		}
		
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

		ContractStampProfileManager.splitContractStampProfile(contract, splitDate);
		flash.success("Tipo timbratura suddivisa in due sottoperiodi con valore %s.", contract.fixedworkingtime);
		Persons.edit(contract.contract.person.id);

	}

	public static void deleteContractStampProfile(Long contractStampProfileId){
		ContractStampProfile csp = ContractDao.getContractStampProfileById(contractStampProfileId);
		if(csp==null){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(csp.contract.person.office);

		Contract contract = csp.contract;

		int index = ContractManager.getContractStampProfileAsList(contract).indexOf(csp);
		if(ContractManager.getContractStampProfileAsList(contract).size()<index){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Persons.edit(csp.contract.person.id);
		}
		ContractStampProfile previous = ContractManager.getContractStampProfileAsList(contract).get(index-1);
		ContractStampProfileManager.deleteContractStampProfile(contract, index, csp);

		//Ricalcolo i valori
		try {
			contractManager.recomputeContract(previous.contract, csp.startFrom, null);

			flash.success("Tipologia di timbratura eliminata correttamente. Tornati alla precedente che ha timbratura automatica con valore: %s", previous.fixedworkingtime);
		
		} catch(EpasExceptionNoSourceData e) {	
			flash.error("Mancano i dati di inizializzazione per " 
    				+ previous.contract.person.fullName());
		}
		
		Persons.edit(csp.contract.person.id);
	}


	public static void modifySendEmail(Long personId){

		Person person = PersonDao.getPersonById(personId);
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
