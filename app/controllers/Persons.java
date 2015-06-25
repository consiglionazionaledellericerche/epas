package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.CompetenceManager;
import manager.ConfGeneralManager;
import manager.ContractManager;
import manager.ContractStampProfileManager;
import manager.ContractWorkingTimeTypeManager;
import manager.OfficeManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.Qualification;
import models.Role;
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
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class} )
public class Persons extends Controller {

	//	private final static String USERNAME_SESSION_KEY = "username";
	private final static Logger log = LoggerFactory.getLogger(Persons.class);

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static OfficeManager officeManager;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static ContractManager contractManager;
	@Inject
	private static SecurityRules rules;
	@Inject
	private static WorkingTimeTypeDao workingTimeTypeDao;
	@Inject
	private static PersonManager personManager;
	@Inject
	private static ContractDao contractDao;
	@Inject
	private static QualificationDao qualificationDao;
	@Inject
	private static CompetenceManager competenceManager;
	@Inject
	private static ContractStampProfileManager contractStampProfileManager;
	@Inject
	private static PersonDayManager personDayManager;
	@Inject
	private static UserDao userDao;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static ConfGeneralManager confGeneralManager;
	@Inject
	private static ContractWorkingTimeTypeManager contractWorkingTimeTypeManager;
	@Inject
	private static PersonChildrenDao personChildrenDao;

	public static void list(String name){
		
		List<Person> simplePersonList = personDao.listFetched(Optional.fromNullable(name),
				officeDao.getOfficeAllowed(Security.getUser().get()), false, null,
				null, false).list();

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
			@Valid @Required Contract contract,@Required String userName) {

		if(Validation.hasErrors()) {

			flash.error("Inserire correttamente tutti i parametri");
			params.flash(); // add http parameters to the flash scope
			render("@insertPerson", person, qualification, office);
		}

		rules.checkIfPermitted(office);

		person.qualification = qualification;
		person.office = office;
		
		User user = new User();
		user.username = userName;
		
		//generate random token
		SecureRandom random = new SecureRandom();
		user.password = Codec.hexMD5(new BigInteger(130, random).toString(32)) ;
		
		user.save();
		
		person.user = user;
		person.save();
		
		Role employee = Role.find("byName", Role.EMPLOYEE).first();
		officeManager.setUro(person.user, person.office, employee);
		
		contract.person = person;

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeByDescription("Normale");
		
		if( !contractManager.properContractCreate(contract, wtt)) {
			flash.error("Errore durante la creazione del contratto. "
					+ "Assicurarsi di inserire date valide.");
			params.flash(); // add http parameters to the flash scope
			edit(person.id);
		}
		
		person.save();
		
		flash.success("Persona inserita correttamente in anagrafica - %s", person.fullName());
		
		list(null);
	}

	@Deprecated
	public static void insertUsername(Long personId){
		Person person = personDao.getPersonById(personId);
		if(person==null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(person.office);

		List<String> usernameList = personManager.composeUsername(person.name, person.surname);

		render(person, usernameList);
	}

	@Deprecated
	public static void updateUsername(Long personId, String username){

		Person person = personDao.getPersonById(personId);
		if(person==null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
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

		Role employee = Role.find("byName", Role.EMPLOYEE).first();
		officeManager.setUro(person.user, person.office, employee);

		flash.success("%s %s inserito in anagrafica con il valore %s come username", 
				person.name, person.surname, person.user.username);
		list(null);

	}

	public static void edit(Long personId){

		Person person = personDao.getPersonById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(person.office);

		List<IWrapperContract> contractList = FluentIterable
				.from(contractDao.getPersonContractList(person))
				.transform(wrapperFunctionFactory.contract()).toList();
		
		Set<Office> officeList = officeDao.getOfficeAllowed(Security.getUser().get());

		List<ContractStampProfile> contractStampProfileList =
				contractDao.getPersonContractStampProfile(Optional.fromNullable(person), 
						Optional.<Contract>absent());

		LocalDate actualDate = new LocalDate();
		Integer month = actualDate.getMonthOfYear();
		Integer year = actualDate.getYear();

		Long id = person.id;
		render(person, contractList, contractStampProfileList, month, year, id, actualDate, officeList);
	}

	public static void update(@Valid Person person, Office office, Integer qualification, boolean isPersonInCharge){
		
		if(person==null) {
			flash.error("La persona da modificare non esiste. Operazione annullata");
			list(null);
		}
		
		if (Validation.hasErrors()) {
			log.warn("validation errors for {}: {}", person,
					validation.errorsMap());
			flash.error("Impossibile salvare la persona %s, verificare i parametri",person);
			edit(person.id);
		}
		
		rules.checkIfPermitted(person.office);
		rules.checkIfPermitted(office);

		if(office!=null) {
			person.office = office;
		}

		Optional<Qualification> q = qualificationDao.byQualification(qualification);
		if( !q.isPresent() ) {
			flash.error("La qualifica selezionata non esiste. Operazione annullata");
			list(null);
		}
		person.isPersonInCharge = isPersonInCharge;
		person.qualification = q.get();
		person.save();
		flash.success("Modificate informazioni per l'utente %s %s", person.name, person.surname);

		// FIXME: la modifica della persona dovrebbe far partire qualche ricalcolo??
		// esempio qualifica, office possono far cambiare qualche decisione dell'alg.
		
		edit(person.id);
	}

	public static void deletePerson(Long personId){
		Person person = personDao.getPersonById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(person.office);

		render(person);
	}

	
	@SuppressWarnings("deprecation")
	public static void deletePersonConfirmed(Long personId){
		Person person = personDao.getPersonById(personId);
		if(person == null) {

			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(person.office);

		/***** person.delete(); ******/

		if(person.user != null) {

			if(person.user.usersRolesOffices.size() > 0) {
				flash.error("Impossibile eliminare una persona che detiene diritti di amministrazione su almeno una sede. Rimuovere tali diritti e riprovare.");
				list(null);
			}

			if(person.user.username.equals(Security.getUser().get().username)) {

				flash.error("Impossibile eliminare la persona loggata nella sessione corrente. Operazione annullata.");
				list(null);
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
		contractManager.deletePersonContracts(person);

		// Eliminazione assenze e tempi da inizializzazione
		contractManager.deleteInitializations(person);

		JPAPlugin.closeTx(false);
		log.debug("Elimino timbrature e dati mensili e annuali...");
		JPAPlugin.startTx(false);
		person = personDao.getPersonById(personId);
		// Eliminazione figli in anagrafica
		personManager.deletePersonChildren(person);

		// Eliminazione person day
		personDayManager.deletePersonDays(person);

		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		person = personDao.getPersonById(personId);
		//Eliminazione riepiloghi annuali

		// Eliminazione reperibilità turni e ore di formazione e riepiloghi annuali
		personManager.deleteShiftReperibilityTrainingHoursAndYearRecap(person);

		JPAPlugin.closeTx(false);

		// Eliminazione persona e user
		JPAPlugin.startTx(false);
		person = personDao.getPersonById(personId);

		Long userId = null;
		if(person.user != null) {
			userId = person.user.id;
		}
		person.delete();
		JPAPlugin.closeTx(false);

		JPAPlugin.startTx(false);
		if(userId != null) {
			User user = userDao.getUserById(userId, Optional.<String>absent());
			user.delete();
		}
		JPAPlugin.closeTx(false);

		flash.success("La persona %s %s eliminata dall'anagrafica insieme a tutti i suoi dati.",name, surname);

		list(null);

	}

	public static void showCurrentVacation(Long personId){

		Person person = personDao.getPersonById(personId);
		if(person == null) {
			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(person.office);

		IWrapperPerson wPerson = wrapperFactory.create(person);

		Preconditions.checkState(wPerson.getCurrentVacationPeriod().isPresent());

		VacationPeriod vp = wPerson.getCurrentVacationPeriod().get();
		render(person, vp);
	}

	public static void showCurrentContractWorkingTimeType(Long personId) {

		Person person = personDao.getPersonById(personId);

		if(person == null) {
			flash.error("La persona selezionata non esiste. Operazione annullata");
			list(null);
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
			list(null);
		}

		rules.checkIfPermitted(person.office);

		Contract con = new Contract();
		List<WorkingTimeType> wttList = workingTimeTypeDao.getAllWorkingTimeType();
		render(con, person, wttList);
	}

	public static void saveContract(@Required LocalDate dataInizio, 
			@Valid LocalDate dataFine, Person person, WorkingTimeType wtt,
			boolean onCertificate) {

		//Controllo parametri
		if(person==null) {

			flash.error("Persona inesistente. Operazione annullata.");
			list(null);
		}

		rules.checkIfPermitted(person.office);

		if(dataInizio==null) {

			flash.error("Errore nel fornire il parametro data inizio contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(person.id);
		}
		if(Validation.hasErrors()) {

			flash.error("Errore nel fornire il parametro data fine contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(person.id);
		}

		//Tipo orario
		if(wtt == null) {
			flash.error("Errore nel fornire il parametro tipo orario. "
					+ "Operazione annullata.");
			edit(person.id);
		}

		//Creazione nuovo contratto
		Contract contract = new Contract();
		contract.beginContract = dataInizio;
		contract.expireContract = dataFine;
		contract.onCertificate = onCertificate;
		contract.person = person;
		
		if( !contractManager.properContractCreate(contract, wtt)) {
			flash.error("Errore durante la creazione del contratto. "
					+ "Assicurarsi di inserire date valide e che non si "
					+ "sovrappongono con altri contratti della person");
			params.flash(); // add http parameters to the flash scope
			edit(person.id);
		}
		
		flash.success("Il contratto per %s %s è stato correttamente salvato", 
				person.name, person.surname);

		edit(person.id);
	}

	public static void modifyContract(Long contractId){
		Contract contract = contractDao.getContractById(contractId);
		if(contract == null) {
			flash.error("Non è stato trovato nessun contratto con id %s per il dipendente ", contractId);
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		render(contract);
	}

	public static void updateContract(Contract contract, @Required LocalDate begin, 
			@Valid LocalDate expire, @Valid LocalDate end, boolean onCertificate){

		//Controllo dei parametri
		if(contract == null) {

			flash.error("Contratto inesistente, operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		if(begin==null){

			flash.error("Errore nel fornire il parametro data inizio contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(contract.person.id);
		}
		if(validation.hasError("expire")) {

			flash.error("Errore nel fornire il parametro data fine contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(contract.person.id);
		}
		if(validation.hasError("end")) {

			flash.error("Errore nel fornire il parametro data terminazione contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(contract.person.id);
		}

		contract.beginContract = begin;
		contract.expireContract = expire;
		contract.endContract = end;

		//Date non si sovrappongono con gli altri contratti della persona
		if( ! contractManager.isProperContract(contract) ) {

			flash.error("Il contratto si interseca con altri contratti della persona. "
					+ "Controllare le date di inizio e fine. Operazione annulalta.");
			edit(contract.person.id);
		}

		contract.onCertificate = onCertificate;

		contractManager.properContractUpdate(contract);

		contract.save();

		flash.success("Aggiornato contratto per il dipendente %s %s", 
				contract.person.name, contract.person.surname);

		edit(contract.person.id);

	}

	public static void deleteContract(Long contractId){

		Contract contract = contractDao.getContractById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		render(contract);
	}

	public static void deleteContractConfirmed(Long contractId){

		Contract contract = contractDao.getContractById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		for(ContractStampProfile csp : contract.contractStampProfile){
			csp.delete();
		}

		contract.delete();

		flash.error("Contratto eliminato con successo.");
		edit(contract.person.id);
	}

	public static void updateSourceContract(Long contractId){

		Contract contract = contractDao.getContractById(contractId);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		Optional<LocalDate> initUse = confGeneralManager.getLocalDateFieldValue(Parameter.INIT_USE_PROGRAM, 
				Security.getUser().get().person.office);
		
		//Preconditions.checkState(initUse.isPresent());

		render(contract, initUse);
	}


	public static void saveSourceContract(Contract contract) {

		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		contractManager.saveSourceContract(contract);

		//Ricalcolo valori
		DateInterval contractDateInterval = wrapperFactory.create(contract).getContractDateInterval();

		contractManager.recomputeContract(contract, contractDateInterval.getBegin(), contractDateInterval.getEnd());

		flash.success("Dati di inizializzazione definiti con successo ed effettuati i ricalcoli.");

		edit(contract.person.id);

	}


	public static void updateContractWorkingTimeType(Long id){

		Contract contract = contractDao.getContractById(id);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			list(null);
		}

		rules.checkIfPermitted(contract.person.office);

		//La lista dei tipi orario ammessi per la persona
		List<WorkingTimeType> wttDefault = workingTimeTypeDao.getDefaultWorkingTimeType();
		List<WorkingTimeType> wttAllowed = contract.person.office.getEnabledWorkingTimeType();
		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		wttList.addAll(wttDefault);
		wttList.addAll(wttAllowed);
		
		IWrapperContract wrappedContract = wrapperFactory.create(contract);
		
		render(wrappedContract,contract, wttList);
	}

	public static void splitContractWorkingTimeType(ContractWorkingTimeType cwtt, LocalDate splitDate){
		
		//Controllo integrità richiesta
		if(cwtt==null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(cwtt.contract.person.office);

		if(validation.hasError("splitDate")) {

			flash.error("Errore nel fornire il parametro data. Inserire la data nel corretto formato aaaa-mm-gg");
			edit(cwtt.contract.person.id);
		}

		if(!DateUtility.isDateIntoInterval(splitDate, new DateInterval(cwtt.beginDate, cwtt.endDate))) {

			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			edit(cwtt.contract.person.id);
		}

		DateInterval first = new DateInterval(cwtt.beginDate, splitDate.minusDays(1));
		if(! DateUtility.isIntervalIntoAnother(first, wrapperFactory.create(cwtt.contract).getContractDateInterval())) {
			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			edit(cwtt.contract.person.id);
		}
		//agire
		contractWorkingTimeTypeManager.saveSplitContractWorkingTimeType(cwtt, splitDate);

		flash.success("Orario di lavoro correttamente suddiviso in due sottoperiodi con tipo orario %s.", cwtt.workingTimeType.description);
		edit(cwtt.contract.person.id);
	}

	public static void deleteContractWorkingTimeType(ContractWorkingTimeType cwtt){
		if(cwtt==null){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(cwtt.contract.person.office);

		Contract contract = cwtt.contract;

		List<ContractWorkingTimeType> contractsWtt = Lists.newArrayList(contract.contractWorkingTimeType);

		int index = contractsWtt.indexOf(cwtt);
		if(contractsWtt.size()<index){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			edit(cwtt.contract.person.id);
		}
		ContractWorkingTimeType previous = contractsWtt.get(index-1);
		contractWorkingTimeTypeManager.deleteContractWorkingTimeType(contract, index, cwtt);

		contractManager.recomputeContract(cwtt.contract, cwtt.beginDate, null);

		flash.success("Orario di lavoro eliminato correttamente. Attribuito al periodo eliminato il tipo orario %s.", previous.workingTimeType.description);

		edit(cwtt.contract.person.id);
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
		contractManager.recomputeContract(cwtt.contract, cwtt.beginDate, null);

		flash.success("Cambiato correttamente tipo orario per il periodo a %s.", cwtt.workingTimeType.description);

		edit(cwtt.contract.person.id);

	}

	public static void changePassword(){
		User user = Security.getUser().get();
		notFoundIfNull(user);
		render(user);
	}

	public static void savePassword(@Required String vecchiaPassword,
			@MinLength(5) @Required String nuovaPassword, @MinLength(5) @Required String confermaPassword){

		User user = userDao.getUserByUsernameAndPassword(Security.getUser().get().username, Optional.fromNullable(Hashing.md5().hashString(vecchiaPassword,  Charsets.UTF_8).toString()));

		if(user == null) {
			flash.error("Nessuna corrispondenza trovata fra utente e vecchia password inserita.");
			changePassword();
		}

		if(validation.hasErrors() || !nuovaPassword.equals(confermaPassword)) {
			flash.error("Tutti i campi devono essere valorizzati. "
					+ "La passord deve essere almeno lunga 5 caratteri. Operazione annullata.");
			changePassword();
		}

		notFoundIfNull(user);

		Codec codec = new Codec();

		user.password = codec.hexMD5(nuovaPassword);
		user.save();
		flash.success(Messages.get("passwordSuccessfullyChanged"));
		changePassword();
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

		Person person = personDao.getPersonById(personId);
		render(person);
	}

	public static void insertChild(Long personId){

		Person person = personDao.getPersonById(personId);
		notFoundIfNull(person);
		rules.checkIfPermitted(person.office);
		render(person);
	}

	public static void editChild(Long childId){

		PersonChildren child = personChildrenDao.getById(childId);
		notFoundIfNull(child);
		Person person = child.person;

		render("@insertChild", child, person);
	}

	public static void removeChild(Long childId){

		PersonChildren child = personChildrenDao.getById(childId);
		notFoundIfNull(child);
		Person person = child.person;

		render(child, person);
	}

	public static void deleteChild(Long childId){

		PersonChildren child = personChildrenDao.getById(childId);
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
			for(PersonChildren p : personChildrenDao.getAllPersonChildren(person)){

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

		ContractStampProfile contract = contractDao.getContractStampProfileById(id);
		if(contract == null) {

			flash.error("Contratto inesistente. Operazione annullata.");
			list(null);
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

		contractManager.recomputeContract(contract.contract, contract.startFrom, null);

		flash.success("Cambiata correttamente tipologia di timbratura per il periodo a %s.", newtipo);

		edit(contract.contract.person.id);

	}

	public static void splitContractStampProfile(ContractStampProfile contract, LocalDate splitDate){
		if(contract==null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(contract.contract.person.office);

		if(validation.hasError("splitDate")) {

			flash.error("Errore nel fornire il parametro data. Inserire la data nel corretto formato aaaa-mm-gg");
			edit(contract.contract.person.id);
		}

		if(!DateUtility.isDateIntoInterval(splitDate, new DateInterval(contract.startFrom, contract.endTo))) {

			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			edit(contract.contract.person.id);
		}
		DateInterval first = new DateInterval(contract.startFrom, splitDate.minusDays(1));
		if(! DateUtility.isIntervalIntoAnother(first,  wrapperFactory.create(contract.contract).getContractDateInterval())) {
			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			edit(contract.contract.person.id);
		}

		contractStampProfileManager.splitContractStampProfile(contract, splitDate);
		flash.success("Tipo timbratura suddivisa in due sottoperiodi con valore %s.", contract.fixedworkingtime);
		edit(contract.contract.person.id);

	}

	public static void deleteContractStampProfile(Long contractStampProfileId){
		ContractStampProfile csp = contractDao.getContractStampProfileById(contractStampProfileId);
		if(csp==null){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(csp.contract.person.office);

		Contract contract = csp.contract;

		int index = contractManager.getContractStampProfileAsList(contract).indexOf(csp);
		if(contractManager.getContractStampProfileAsList(contract).size()<index){

			flash.error("Impossibile completare la richiesta, controllare i log.");
			edit(csp.contract.person.id);
		}
		ContractStampProfile previous = contractManager.getContractStampProfileAsList(contract).get(index-1);
		contractStampProfileManager.deleteContractStampProfile(contract, index, csp);

		//Ricalcolo i valori
		contractManager.recomputeContract(previous.contract, csp.startFrom, null);

		flash.success("Tipologia di timbratura eliminata correttamente. Tornati alla precedente che ha timbratura automatica con valore: %s", previous.fixedworkingtime);

		edit(csp.contract.person.id);
	}


	public static void modifySendEmail(Long personId){

		Person person = personDao.getPersonById(personId);
		rules.checkIfPermitted(person.office);
		render(person);
	}

	public static void updateSendEmail(Person person, boolean wantEmail){
		if(person == null) {

			flash.error("Persona inesistente, operazione annullata");
			list(null);
		}

		rules.checkIfPermitted(person.office);
		person.wantEmail = wantEmail;
		person.save();
		flash.success("Cambiata gestione di invio mail al dipendente %s %s", person.name, person.surname);
		edit(person.id);
	}
	
	public static void workGroup(Long personId){
		Person person = personDao.getPersonById(personId);
		Set<Office> offices = Sets.newHashSet();
		offices.add(person.office);
		List<Person> people = personDao.list(Optional.<String>absent(), 
				offices, false, LocalDate.now(), LocalDate.now(), true).list();
		render(people, person);
	}
	
	
	public static void confirmGroup(@Required List<Long> peopleId, Long personId){
		Person person = personDao.getPersonById(personId);
		Person p = null;
		for(Long id : peopleId){
			p = personDao.getPersonById(id);
			p.personInCharge = person;
			p.save();
			person.people.add(p);
		}
		person.save();
		flash.success("Aggiunte persone al gruppo di %s %s", person.name, person.surname);
		list(null);
	}
	
	public static void removePersonFromGroup(Long pId){

		Person person = personDao.getPersonById(pId);
		Person supervisor = personDao.getPersonInCharge(person);
		person.personInCharge = null;

		supervisor.save();
		person.save();
		flash.success("Rimosso %s %s dal gruppo di %s %s", person.name, person.surname, supervisor.name, supervisor.surname);
		workGroup(supervisor.id);
	}

}
