package controllers;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.gdata.util.common.base.Preconditions;
import dao.ContractDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import lombok.extern.slf4j.Slf4j;
import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.ContractStampProfileManager;
import manager.ContractWorkingTimeTypeManager;
import manager.EmailManager;
import manager.OfficeManager;
import manager.PersonManager;
import manager.SecureManager;
import manager.UserManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.Role;
import models.User;
import models.VacationPeriod;
import models.WorkingTimeType;
import net.sf.oval.constraint.MinLength;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@Slf4j
@With( {Resecure.class, RequestInit.class} )
public class Persons extends Controller {

	//	private final static String USERNAME_SESSION_KEY = "username";
	@Inject
	private static UserManager userManager;
	@Inject
	private static EmailManager emailManager;
	@Inject
	private static SecureManager secureManager;
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
	private static ContractStampProfileManager contractStampProfileManager;
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
	@Inject
	private static ConsistencyManager consistencyManager;

	public static void list(String name){
		
		List<Person> simplePersonList = personDao.listFetched(
				Optional.fromNullable(name),
				secureManager.officesReadAllowed(Security.getUser().get()),
				false, null, null, false).list();

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
			@Valid Contract contract) {

		if (contract.expireContract != null 
				&& !contract.expireContract.isAfter(contract.beginContract)) {
			Validation.addError("contract.expireContract",
					"Dev'essere successivo all'inizio del contratto");
		}

		if(Validation.hasErrors()) {
			flash.error("Correggere gli errori indicati");
			render("@insertPerson", person,contract);
		}

		rules.checkIfPermitted(person.office);
		
		person.user = personManager.createUser(person);

		person.save();
		
		Role employee = Role.find("byName", Role.EMPLOYEE).first();
		officeManager.setUro(person.user, person.office, employee);
		
		contract.person = person;

		WorkingTimeType wtt = workingTimeTypeDao
				.workingTypeTypeByDescription("Normale", Optional.<Office>absent());
		
		if( !contractManager.properContractCreate(contract, wtt)) {
			flash.error("Errore durante la creazione del contratto. "
					+ "Assicurarsi di inserire date valide.");
			params.flash(); // add http parameters to the flash scope
			edit(person.id);
		}
		
		person.save();

		userManager.generateRecoveryToken(person);
		emailManager.newUserMail(person);

		log.info("Creata nuova persona: id[{}] - {}", person.id, person.fullName());
		
		flash.success("Persona inserita correttamente in anagrafica - %s", person.fullName());
		
		list(null);
	}

	public static void edit(Long personId){

		Person person = personDao.getPersonById(personId);
		notFoundIfNull(person);

		rules.checkIfPermitted(person.office);

		render(person);
	}

	public static void update(@Valid Person person){
		
		notFoundIfNull(person);
		
		if (Validation.hasErrors()) {
			log.warn("validation errors for {}: {}", person,
					validation.errorsMap());
			flash.error("Correggere gli errori indicati.");
			Validation.keep();
			edit(person.id);
		}
		
		rules.checkIfPermitted(person.office);

		person.save();
		flash.success("Modificate informazioni per l'utente %s %s", person.name, person.surname);

		// FIXME: la modifica della persona dovrebbe far partire qualche ricalcolo??
		// esempio qualifica, office possono far cambiare qualche decisione dell'alg.
		
		edit(person.id);
	}

	public static void deletePerson(Long personId) {
		
		Person person = personDao.getPersonById(personId);

		notFoundIfNull(person);

		rules.checkIfPermitted(person.office);

		render(person);
	}

	
	@SuppressWarnings("deprecation")
	public static void deletePersonConfirmed(Long personId){
		
		Person person = personDao.getPersonById(personId);
		
		notFoundIfNull(person);
		
		// FIX per oggetto in entityManager monco.
		person.refresh();
		
		rules.checkIfPermitted(person.office);
		
		// FIXME: se non spezzo in transazioni errore HashMap.
		// Per adesso spezzo l'eliminazione della persona in tre fasi.
		// person.delete() senza errore HashMap dovrebbe però essere sufficiente.
		
		for(Contract c : person.contracts) {
			c.delete();
		}
		
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		person = personDao.getPersonById(personId);
		
		for(PersonDay pd : person.personDays) {
			pd.delete();
		}
		
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		person = personDao.getPersonById(personId);
		
		person.delete();

		flash.success("La persona %s %s eliminata dall'anagrafica"
				+ " insieme a tutti i suoi dati.",person.name, person.surname);

		list(null);

	}

	public static void showCurrentVacation(Long personId){

		Person person = personDao.getPersonById(personId);
		
		notFoundIfNull(person);

		rules.checkIfPermitted(person.office);

		IWrapperPerson wPerson = wrapperFactory.create(person);

		Preconditions.checkState(wPerson.getCurrentVacationPeriod().isPresent());

		VacationPeriod vp = wPerson.getCurrentVacationPeriod().get();
		render(person, vp);
	}

	public static void showCurrentContractWorkingTimeType(Long personId) {

		Person person = personDao.getPersonById(personId);

		notFoundIfNull(person);

		rules.checkIfPermitted(person.office);

		IWrapperPerson wPerson = wrapperFactory.create(person);

		Preconditions.checkState(wPerson.getCurrentContractWorkingTimeType().isPresent());

		ContractWorkingTimeType cwtt = wPerson.getCurrentContractWorkingTimeType().get();

		WorkingTimeType wtt = cwtt.workingTimeType;

		render(person, cwtt, wtt);
	}

	
	public static void saveContract(@Required LocalDate dataInizio, 
			@Valid LocalDate dataFine, Person person, WorkingTimeType wtt,
			boolean onCertificate) {

		//Controllo parametri
		notFoundIfNull(person);

		rules.checkIfPermitted(person.office);

		if (dataInizio == null) {

			flash.error("Errore nel fornire il parametro data inizio contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(person.id);
		}
		if (Validation.hasErrors()) {

			flash.error("Errore nel fornire il parametro data fine contratto. "
					+ "Inserire la data nel corretto formato aaaa-mm-gg");
			edit(person.id);
		}

		//Tipo orario
		if (wtt == null) {
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
		
		if (!contractManager.properContractCreate(contract, wtt)) {
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

	
	public static void deleteContract(Long contractId){

		Contract contract = contractDao.getContractById(contractId);
		
		notFoundIfNull(contract);

		rules.checkIfPermitted(contract.person.office);

		render(contract);
	}

	public static void deleteContractConfirmed(Long contractId){

		Contract contract = contractDao.getContractById(contractId);
		
		notFoundIfNull(contract);

		rules.checkIfPermitted(contract.person.office);

		for(ContractStampProfile csp : contract.contractStampProfile){
			csp.delete();
		}

		contract.delete();

		flash.error("Contratto eliminato con successo.");
		edit(contract.person.id);
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

	public static void resetPassword(@MinLength(5) @Required String nuovaPassword, 
			@MinLength(5) @Required String confermaPassword) throws Throwable {		

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
			flash.error("Correggere gli errori riportati.");
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
				child.name, child.surname, person);
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

	public static void changeTypeOfContractStampProfile(
			ContractStampProfile contract, String newtipo) {
		
		if (contract == null || newtipo == null) {

			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}

		rules.checkIfPermitted(contract.contract.person.office);
		
		if(newtipo.equals("Timbratura automatica")) {
			contract.fixedworkingtime = true;
		} else {
			contract.fixedworkingtime = false;
		}

		contract.save();

		contractManager.recomputeContract(contract.contract, 
				Optional.fromNullable(contract.startFrom), false);

		flash.success("Cambiata correttamente tipologia di timbratura "
				+ "per il periodo a %s.", newtipo);

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
		contractManager.recomputeContract(previous.contract, 
				Optional.fromNullable(csp.startFrom), false);

		flash.success("Tipologia di timbratura eliminata correttamente. "
				+ "Tornati alla precedente che ha timbratura automatica "
				+ "con valore: %s", previous.fixedworkingtime);

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
