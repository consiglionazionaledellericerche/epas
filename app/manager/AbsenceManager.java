package manager;

import it.cnr.iit.epas.CheckMessage;
import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import javax.inject.Inject;

import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.Qualification;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.ConfigurationFields;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.QualificationMapping;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.Blob;
import play.libs.Mail;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.WorkingTimeTypeDao;


/**
 * 
 * @author alessandro
 *
 */
public class AbsenceManager {
	

	private final static Logger log = LoggerFactory.getLogger(AbsenceManager.class);

	@Inject
	public PersonResidualYearRecapFactory yearFactory;
	
	@Inject
	public ContractYearRecapManager contractYearRecapManager;
	
	@Inject
	public WorkingTimeTypeDao workingTimeTypeDao;
	
	@Inject
	public PersonDayManager personDayManager;
	
	@Inject
	public PersonDayDao personDayDao;
	
	@Inject
	public VacationManager vacationManager;
	
	@Inject
	public VacationsRecapFactory vacationsFactory;
	
	private static final String DATE_NON_VALIDE = "L'intervallo di date specificato non è corretto";

	public enum AbsenceToDate implements Function<Absence, LocalDate>{
		INSTANCE;

		@Override
		public LocalDate apply(Absence absence){
			return absence.personDay.date;
		}
	}

	/**
	 * Il primo codice utilizzabile per l'anno selezionato come assenza nel seguente ordine 31,32,94
	 * @param person
	 * @param actualDate
	 * @return
	 */
	private AbsenceType whichVacationCode(Person person, LocalDate date){

		VacationsRecap vr = vacationsFactory.create(date.getYear(),
				ContractDao.getCurrentContract(person), date, true);

		if(vr.vacationDaysLastYearNotYetUsed > 0)
			return AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode());

		if(vr.persmissionNotYetUsed > 0)

			return AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode());

		if(vr.vacationDaysCurrentYearNotYetUsed > 0)
			return AbsenceTypeDao.getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode());


		return null;
	}

	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 32.
	 * @param person
	 * @param date
	 * @return l'absenceType 32 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	private boolean canTake32(Person person, LocalDate date) {

		VacationsRecap vr = vacationsFactory.create(date.getYear(),
				ContractDao.getCurrentContract(person), date, true);

		return (vr.vacationDaysCurrentYearNotYetUsed > 0);		

	}

	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 31.
	 * @param person
	 * @param date
	 * @return true in caso affermativo, false altrimenti
	 * 
	 */
	private boolean canTake31(Person person, LocalDate date) {

		VacationsRecap vr = vacationsFactory.create(date.getYear(),
				ContractDao.getCurrentContract(person), date, true);
		
		return (vr.vacationDaysLastYearNotYetUsed > 0);
	}

	/**
	 * Verifica che la persona alla data possa prendere un giorno di permesso codice 94.
	 * @param person
	 * @param date
	 * @return l'absenceType 94 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	private boolean canTake94(Person person, LocalDate date) {

		VacationsRecap vr = vacationsFactory.create(date.getYear(),
				ContractDao.getCurrentContract(person), date, true);

		return (vr.persmissionNotYetUsed > 0);

	}

	
	/**
	 * Verifica la possibilità che la persona possa usufruire di un riposo compensativo nella data specificata.
	 * Se voglio inserire un riposo compensativo per il mese successivo a oggi considero il residuo a ieri.
	 * N.B Non posso inserire un riposo compensativo oltre il mese successivo a oggi.
	 * @param person
	 * @param date
	 * @return 
	 */
	private boolean canTakeCompensatoryRest(Person person, LocalDate date){
		//Data da considerare 

		// (1) Se voglio inserire un riposo compensativo per il mese successivo considero il residuo a ieri.
		//N.B Non posso inserire un riposo compensativo oltre il mese successivo.
		LocalDate dateToCheck = date;
		//Caso generale
		if( dateToCheck.getMonthOfYear() == LocalDate.now().getMonthOfYear() + 1){
			dateToCheck = LocalDate.now();
		}
		//Caso particolare dicembre - gennaio
		else if( dateToCheck.getYear() == LocalDate.now().getYear() + 1 
				&& dateToCheck.getMonthOfYear() == 1 && LocalDate.now().getMonthOfYear() == 12){
			dateToCheck = LocalDate.now();
		}

		// (2) Calcolo il residuo alla data precedente di quella che voglio considerare.
		if(dateToCheck.getDayOfMonth()>1)
			dateToCheck = dateToCheck.minusDays(1);

		//Contract contract = person.getContract(dateToCheck);
		Contract contract = ContractDao.getContract(dateToCheck, person);

		PersonResidualYearRecap c = 
				yearFactory.create(contract, dateToCheck.getYear(), dateToCheck);

		if(c == null){
			return false;
		}

		PersonResidualMonthRecap mese = c.getMese(dateToCheck.getMonthOfYear());

		if(mese.monteOreAnnoCorrente + mese.monteOreAnnoPassato 
				> //mese.person.getWorkingTimeType(dateToCheck).getWorkingTimeTypeDayFromDayOfWeek(dateToCheck.getDayOfWeek()).workingTime) {
			workingTimeTypeDao.getWorkingTimeType(dateToCheck, person)
				.workingTimeTypeDays.get(dateToCheck.getDayOfWeek()-1).workingTime ) {
			return true;
		} 
		return false;	
	}

	/**
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @param mealTicket
	 * @return
	 */
	public AbsenceInsertReport insertAbsence(Person person, LocalDate dateFrom,Optional<LocalDate> dateTo, 
			AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket){

		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(absenceType);
		Preconditions.checkNotNull(dateFrom);
		Preconditions.checkNotNull(dateTo);
		Preconditions.checkNotNull(file);
		Preconditions.checkNotNull(mealTicket);

		log.info("Ricevuta richiesta di inserimento assenza per {}. AbsenceType = {} dal {} al {}, mealTicket = {}. Attachment = {}",
				new Object[] {person.fullName(), absenceType.code, dateFrom, dateTo.or(dateFrom), mealTicket.orNull(), file.orNull()});

		AbsenceInsertReport air = new AbsenceInsertReport();

		if(dateTo.isPresent() && dateFrom.isAfter(dateTo.get())){
			air.getWarnings().add(DATE_NON_VALIDE);
			air.getDatesInTrouble().add(dateFrom);
			air.getDatesInTrouble().add(dateTo.get());
			return air;
		}

		List<Absence> absenceTypeAlreadyExisting = absenceTypeAlreadyExist(
				person, dateFrom, dateTo.or(dateFrom), absenceType);
		if (absenceTypeAlreadyExisting.size() > 0) {
			air.getWarnings().add(AbsencesResponse.CODICE_FERIE_GIA_PRESENTE);
			air.getDatesInTrouble().addAll(Collections2.transform(absenceTypeAlreadyExisting, AbsenceToDate.INSTANCE));
			return air;
		}

		List<Absence> allDayAbsenceAlreadyExisting = AbsenceDao.allDayAbsenceAlreadyExisting(person, dateFrom, dateTo);
		if (allDayAbsenceAlreadyExisting.size() > 0) {
			air.getWarnings().add(AbsencesResponse.CODICE_GIORNALIERO_GIA_PRESENTE);
			air.getDatesInTrouble().addAll(Collections2.transform(allDayAbsenceAlreadyExisting, AbsenceToDate.INSTANCE));
			return air;
		}

		LocalDate actualDate = dateFrom;

		while(!actualDate.isAfter(dateTo.or(dateFrom))){

			if (AbsenceTypeMapping.RIPOSO_COMPENSATIVO.is(absenceType)) {
				air.add(handlerCompensatoryRest(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			if(AbsenceTypeMapping.FER.is(absenceType)){
				air.add(handlerFER(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			if(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(absenceType) || 
					AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(absenceType) ||
					AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(absenceType)){
				air.add(handler31_32_94(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;

			}
			if(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.is(absenceType)){
				air.add(handler37(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			//TODO Inserire i codici di assenza necessari nell'AbsenceTypeMapping
			if((absenceType.code.startsWith("12") || absenceType.code.startsWith("13")) && absenceType.code.length() == 3){
				air.add(handlerChildIllness(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			if(absenceType.absenceTypeGroup != null){
				for(AbsencesResponse ar : handlerAbsenceTypeGroup(person, actualDate, absenceType, file))
					air.add(ar);
				actualDate = actualDate.plusDays(1);
				continue;
			}

			air.add(handlerGenericAbsenceType(person, actualDate, absenceType, file,mealTicket));

			actualDate = actualDate.plusDays(1);
		}
		
		//Al termine dell'inserimento delle assenze aggiorno tutta la situazione dal primo giorno di assenza fino ad oggi
		personDayManager.updatePersonDaysFromDate(person, dateFrom);
		
		//Se ho inserito una data in un anno precedente a quello attuale effettuo 
		//il ricalcolo del riepilogo annuale per ogni contratto attivo in quell'anno
		if(dateFrom.getYear() < LocalDate.now().getYear()){
			for(Contract c : PersonDao.getContractList(person, 
					dateFrom.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), 
					dateFrom.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue())){
				contractYearRecapManager.buildContractYearRecap(c);
			}
		}
		
		if(air.getAbsenceInReperibilityOrShift() > 0){
			sendEmail(person, air);
		}					

		return air;
	}

	/**
	 * Inserisce l'assenza absenceType nel person day della persona nella data.
	 *  Se dateFrom = dateTo inserisce nel giorno singolo.
	 * @param person
	 * @param date
	 * @param absenceType
	 * @param file
	 * @return	un resoconto dell'inserimento tramite la classe AbsenceInsertModel
	 */
	private AbsencesResponse insert(Person person, LocalDate date, 
			AbsenceType absenceType, Optional<Blob> file){

		Preconditions.checkNotNull(person);
		Preconditions.checkState(person.isPersistent());
		Preconditions.checkNotNull(date);
		Preconditions.checkNotNull(absenceType);
		Preconditions.checkState(absenceType.isPersistent());
		Preconditions.checkNotNull(file);

		AbsencesResponse ar = new AbsencesResponse(date,absenceType.code);

		//se non devo considerare festa ed è festa non inserisco l'assenza
		if(!absenceType.consideredWeekEnd && PersonManager.isHoliday(person, date)){
			ar.setHoliday(true);
			ar.setWarning(AbsencesResponse.NON_UTILIZZABILE_NEI_FESTIVI);
		}
		else {
			if(checkIfAbsenceInReperibilityOrInShift(person, date)){
				ar.setDayInReperibilityOrShift(true);				
			}

			List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, date, Optional.<LocalDate>absent(), false);
			PersonDay pd = 	FluentIterable.from(personDays).first().or(new PersonDay(person, date));

			if(personDays.isEmpty()){
				pd.create();
			}

			//creo l'assenza e l'aggiungo
			Absence absence = new Absence();
			absence.absenceType = absenceType;
			absence.personDay = pd;
			absence.absenceFile = file.orNull();
			absence.save();

			ar.setAbsenceCode(absenceType.code);
			ar.setInsertSucceeded(true);

			log.info("Inserita nuova assenza {} per {} in data: {}", new Object[]{
					absence.absenceType.code, absence.personDay.person.getFullname(),absence.personDay.date});

			pd.absences.add(absence);
		}
		return ar;
	}

	/**
	 * Controlla che nell'intervallo passato in args non esistano già assenze per quel tipo
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @return
	 */
	private static List<Absence> absenceTypeAlreadyExist(Person person,LocalDate dateFrom,
			LocalDate dateTo, AbsenceType absenceType){

		return AbsenceDao.findByPersonAndDate
				(person, dateFrom, Optional.of(dateTo),
						Optional.of(absenceType)).list();
	}

	/**
	 * Gestisce l'inserimento dei codici 91 (1 o più consecutivi)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @throws EmailException 
	 */
	private AbsencesResponse handlerCompensatoryRest(Person person,
			LocalDate date, AbsenceType absenceType,Optional<Blob> file){

		Integer maxRecoveryDaysOneThree = Integer.parseInt(ConfYearManager.getFieldValue(
				ConfigurationFields.MaxRecoveryDays13.description, date.getYear(), person.office));
//		TODO le assenze con codice 91 non sono sufficienti a coprire tutti i casi.
//		Bisogna considerare anche eventuali inizializzazioni
		int alreadyUsed = 0;
		List<Absence> absences91 = AbsenceDao.getAbsenceByCodeInPeriod(
				Optional.fromNullable(person), Optional.fromNullable(absenceType.code),
				date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(),
				date, Optional.<JustifiedTimeAtWork>absent(), false, false);
		if(absences91 != null){
			alreadyUsed = absences91.size();
		}

// 			verifica se ha esaurito il bonus per l'anno
		if(person.qualification.qualification > 0 && 
				person.qualification.qualification < 4 && 
				alreadyUsed >= maxRecoveryDaysOneThree){
//			TODO	questo è il caso semplice,c'è da considerare anche eventuali cambi di contratto,
//					assenze richieste per gennaio con residui dell'anno precedente sufficienti etc..
			return new AbsencesResponse(date,absenceType.code,
					String.format(AbsencesResponse.RIPOSI_COMPENSATIVI_ESAURITI +
							" - Usati %s", alreadyUsed));
		}
		//Controllo del residuo
		if(canTakeCompensatoryRest(person, date)){
			return insert(person, date, absenceType, file);
		}

		return new AbsencesResponse(date,absenceType.code,
				AbsencesResponse.MONTE_ORE_INSUFFICIENTE);
	}

	/**
	 * metodo che invia la mail contenente i giorni in cui ci sono inserimenti di assenza in turno o reperibilità
	 * @param person
	 * @param cai
	 * @throws EmailException
	 */
	private static void sendEmail(Person person, AbsenceInsertReport airl) {
		MultiPartEmail email = new MultiPartEmail();

		try {
			email.addTo(person.email);
			//Da attivare, commentando la riga precedente, per fare i test così da evitare di inviare mail a caso ai dipendenti...
//			email.addTo("daniele.murgia@iit.cnr.it");
			email.setFrom("epas@iit.cnr.it");
			email.setSubject("Segnalazione inserimento assenza in giorno con reperibilità/turno");
			String date = "";
			for(LocalDate data : airl.datesInReperibilityOrShift()){
				date = date+data+' ';
			}
			email.setMsg("E' stato richiesto l'inserimento di una assenza per il giorno "+date+ 
					" per il quale risulta una reperibilità o un turno attivi. "+'\n'+
					"Controllare tramite la segreteria del personale."+'\n'+
					'\n'+
					"Servizio ePas");

		} catch (EmailException e) {
			// TODO GESTIRE L'Eccezzione nella generazione dell'email
			e.printStackTrace();
		}

		Mail.send(email); 
	}

	/**
	 * controlla se si sta prendendo un codice di assenza in un giorno in cui si è reperibili
	 * @return true se si sta prendendo assenza per un giorno in cui si è reperibili, false altrimenti
	 */
	private static boolean checkIfAbsenceInReperibilityOrInShift(Person person, LocalDate date){

		//controllo se la persona è in reperibilità
		PersonReperibilityDay prd = PersonReperibilityDayDao.getPersonReperibilityDay(person, date);
		//controllo se la persona è in turno
		PersonShiftDay psd = PersonShiftDayDao.getPersonShiftDay(person, date);

		return !(psd == null && prd == null);	
	}

	/**
	 * Gestisce l'inserimento esplicito dei codici 31, 32 e 94.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 */		
	private AbsencesResponse handler31_32_94(Person person,
			LocalDate date, AbsenceType absenceType,Optional<Blob> file){

		if(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(absenceType) && canTake32(person, date)){
			return insert(person, date,absenceType,file);
		}
		if(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(absenceType) && canTake31(person, date)){
			return insert(person, date,absenceType, file);
		}
		if(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(absenceType) && canTake94(person, date)){
			return insert(person, date,absenceType, file);
		}
		//		CODICE FERIE NON DISPONIBILE
		return new AbsencesResponse(date,absenceType.code,
				AbsencesResponse.NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO);
	}

	/**
	 * Gestisce una richiesta di inserimento codice 37 (utilizzo ferie anno precedente scadute)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @throws EmailException 
	 */
	private AbsencesResponse handler37(Person person,
			LocalDate date, AbsenceType absenceType,Optional<Blob> file){

		//FIXME Verificare i controlli d'inserimento
		if(date.getYear() == LocalDate.now().getYear()){

			int remaining37 = vacationManager.remainingPastVacationsAs37(date.getYear(), person);
			if(remaining37 > 0){
				return insert(person, date,absenceType, file);
			}
		}

		return new AbsencesResponse(date,absenceType.code,
				AbsencesResponse.NESSUN_CODICE_FERIE_ANNO_PRECEDENTE_37);
	}

	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @throws EmailException 
	 */
	private List<AbsencesResponse> handlerAbsenceTypeGroup(Person person,LocalDate date,
			AbsenceType absenceType, Optional<Blob> file){

		CheckMessage checkMessage = PersonUtility.checkAbsenceGroup(absenceType, person, date);
		List<AbsencesResponse> result = Lists.newArrayList();

		if(checkMessage.check == false){
			result.add(new AbsencesResponse(date, absenceType.code,AbsencesResponse.ERRORE_GENERICO));
			return result;
		}

		result.add(insert(person, date,absenceType,file));

		if(checkMessage.absenceType != null){
			result.add(insert(person, date,checkMessage.absenceType,file));
		}
		return result;
	}


	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param absenceType
	 * @throws EmailException 
	 */
	private AbsencesResponse handlerChildIllness(Person person,LocalDate date,
			AbsenceType absenceType, Optional<Blob> file){
		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		//TODO: se il dipendente ha più di 9 figli! non funziona dal 10° in poi		
		if(PersonUtility.canTakePermissionIllnessChild(person, date, absenceType)){
			return insert(person, date,absenceType,file);
		}
		//		TODO Completare i controlli nel caso non sia possibile prendere il codice assenza per malattia dei figli
		//		if(esito==null){
		//			//			flash.error("ATTENZIONE! In anagrafica la persona selezionata non ha il numero di figli sufficienti per valutare l'assegnazione del codice di assenza nel periodo selezionato. "
		//			//					+ "Accertarsi che la persona disponga dei privilegi per usufruire dal codice e nel caso rimuovere le assenze inserite.");
		//		}
		//		else if(!esito){
		//			//			flash.error(String.format("Il dipendente %s %s non può prendere il codice d'assenza %s poichè ha già usufruito del numero" +
		//			//					" massimo di giorni di assenza per quel codice o non ha figli che possono usufruire di quel codice", person.name, person.surname, absenceType.code));
		//		}
		return new AbsencesResponse(date,absenceType.code,
				AbsencesResponse.CODICI_MALATTIA_FIGLI_NON_DISPONIBILE);
	}

	/**
	 * Gestisce l'inserimento dei codici FER, 94-31-32 nell'ordine. Fino ad esaurimento.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @throws EmailException 
	 */
	private AbsencesResponse handlerFER(Person person,LocalDate date,
			AbsenceType absenceType, Optional<Blob> file){

		AbsenceType wichFer = whichVacationCode(person, date);

		//FER esauriti
		if(wichFer==null){
			return new AbsencesResponse(date,absenceType.code,
					AbsencesResponse.NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO);
		}
		return insert(person, date, wichFer, file);
	}

	private AbsencesResponse handlerGenericAbsenceType(Person person,LocalDate date,
			AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket){

		AbsencesResponse aim = insert(person, date, absenceType, file);
		if(mealTicket.isPresent()){
			checkMealTicket(date, person, mealTicket.get(), absenceType);
		}
		return aim;
	}

	/**
	 * Gestore della logica ticket forzato dall'amministratore, risponde solo in caso di codice 92
	 * @param date
	 * @param person
	 * @param mealTicket
	 * @param abt
	 */
	private void checkMealTicket(LocalDate date, Person person, String mealTicket, AbsenceType abt){

		PersonDay pd = personDayDao.getPersonDayInPeriod(person, date, Optional.<LocalDate>absent(), false).get(0);

		if(pd == null)
			pd = new PersonDay(person, date);

		if(abt == null || !abt.code.equals("92")){
			pd.isTicketForcedByAdmin = false;	//una assenza diversa da 92 ha per forza campo calcolato
			personDayManager.populatePersonDay(pd);
			return;
		}
		if(mealTicket != null && mealTicket.equals("si")){
			pd.isTicketForcedByAdmin = true;
			pd.isTicketAvailable = true;
			personDayManager.populatePersonDay(pd);
		}
		if(mealTicket != null && mealTicket.equals("no")){
			pd.isTicketForcedByAdmin = true;
			pd.isTicketAvailable = false;
			personDayManager.populatePersonDay(pd);
		}

		if(mealTicket != null && mealTicket.equals("calcolato")){
			pd.isTicketForcedByAdmin = false;
			personDayManager.populatePersonDay(pd);
		}
	}

	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 */
	public int removeAbsencesInPeriod(Person person, LocalDate dateFrom, 
			LocalDate dateTo, AbsenceType absenceType){

		LocalDate today = LocalDate.now();
		LocalDate actualDate = dateFrom;
		int deleted = 0;
		while(!actualDate.isAfter(dateTo)){

			List<PersonDay> personDays = personDayDao.getPersonDayInPeriod(person, actualDate, Optional.<LocalDate>absent(), false);
			PersonDay pd = FluentIterable.from(personDays).first().orNull();

			//Costruisco se non esiste il person day
			if(pd == null){
				actualDate = actualDate.plusDays(1);
				continue;
			}

			List<Absence> absenceList = AbsenceDao.getAbsencesInPeriod(Optional.fromNullable(person), actualDate, Optional.<LocalDate>absent(), false);

			for(Absence absence : absenceList){
				if(absence.absenceType.code.equals(absenceType.code))
				{
					absence.delete();
					pd.absences.remove(absence);
					pd.isTicketForcedByAdmin = false;
					deleted++;
					log.info("Rimossa assenza del {} per {}", actualDate, person.getFullname());
				}
			}
			if(pd.date.isAfter(today) && pd.absences.isEmpty() && pd.absences.isEmpty()){
				pd.delete();
			}
			actualDate = actualDate.plusDays(1);
		}

		//Al termine della cancellazione delle assenze aggiorno tutta la situazione dal primo giorno di assenza fino ad oggi
		personDayManager.updatePersonDaysFromDate(person, dateFrom);

		//Se ho inserito una data in un anno precedente a quello attuale effettuo 
		//il ricalcolo del riepilogo annuale per ogni contratto attivo in quell'anno
		if(dateFrom.getYear() < LocalDate.now().getYear()){
			for(Contract c : PersonDao.getContractList(person, 
					dateFrom.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), 
					dateFrom.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue())){
				contractYearRecapManager.buildContractYearRecap(c);
			}
		}
		return deleted;
	}

	public static boolean AbsenceTypeIsTecnologo(Qualification qualification){
			return QualificationMapping.TECNOLOGI.getRange().contains(qualification.qualification);
	}
	
	public static boolean istechnician(List<Qualification> list){
		for(Qualification q : list){
			return QualificationMapping.TECNICI.getRange().contains(q.qualification);
		}
		return false;
	}

}
