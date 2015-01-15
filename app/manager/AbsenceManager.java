package manager;

import it.cnr.iit.epas.CheckAbsenceInsert;
import it.cnr.iit.epas.CheckMessage;
import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import javax.persistence.Query;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import manager.response.AbsenceInsertResponse;
import manager.response.AbsenceInsertResponseList;
import models.Absence;
import models.AbsenceType;
import models.ConfYear;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.ConfigurationFields;
import models.enumerate.JustifiedTimeAtWork;
import models.rendering.VacationsRecap;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import play.Logger;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.libs.Mail;
import controllers.Stampings;
import controllers.Wizard.WizardStep;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;

/**
 * 
 * @author alessandro
 *
 */
public class AbsenceManager {

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
	private static AbsenceType whichVacationCode(Person person, LocalDate date){

		Contract contract = person.getCurrentContract();

		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}

		if(vr.vacationDaysLastYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "31").first();

		if(vr.persmissionNotYetUsed > 0)
			return AbsenceType.find("byCode", "94").first();

		if(vr.vacationDaysCurrentYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "32").first();

		return null;
	}

	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 32.
	 * @param person
	 * @param date
	 * @return l'absenceType 32 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	private static boolean canTake32(Person person, LocalDate date) {

		Contract contract = person.getCurrentContract();
		VacationsRecap vr = null;
		
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return false;
		}

		return (vr.vacationDaysCurrentYearNotYetUsed > 0);		
	}

	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 31.
	 * @param person
	 * @param date
	 * @return true in caso affermativo, false altrimenti
	 * 
	 */
	private static boolean canTake31(Person person, LocalDate date) {

		Contract contract = person.getCurrentContract();
		VacationsRecap vr = null;

		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return false;
		}

		return (vr.vacationDaysLastYearNotYetUsed > 0);
	}

	/**
	 * Verifica che la persona alla data possa prendere un giorno di permesso codice 94.
	 * @param person
	 * @param date
	 * @return l'absenceType 94 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	private static boolean canTake94(Person person, LocalDate date) {

		Contract contract = person.getCurrentContract();
		VacationsRecap vr = null;
				
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return false;
		}

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
	private static boolean canTakeCompensatoryRest(Person person, LocalDate date){
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

		Contract contract = person.getContract(dateToCheck);

		PersonResidualYearRecap c = 
				PersonResidualYearRecap.factory(contract, dateToCheck.getYear(), dateToCheck);

		if(c == null){
			return false;
		}

		PersonResidualMonthRecap mese = c.getMese(dateToCheck.getMonthOfYear());

		if(mese.monteOreAnnoCorrente + mese.monteOreAnnoPassato 
				> mese.person.getWorkingTimeType(dateToCheck)
				.getWorkingTimeTypeDayFromDayOfWeek(dateToCheck.getDayOfWeek()).workingTime) {
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
	public static AbsenceInsertResponseList insertAbsence(Person person, LocalDate dateFrom,Optional<LocalDate> dateTo, 
			AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket){

		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(absenceType);
		Preconditions.checkNotNull(dateFrom);
		Preconditions.checkNotNull(dateTo);
		Preconditions.checkNotNull(file);
		Preconditions.checkNotNull(mealTicket);

		Logger.info("Ricevuta richiesta di inserimento assenza per %s. AbsenceType = %s, dal %s al %s, mealTicket = %s. Attachment = %s",
				person.fullName(), absenceType.code, dateFrom, dateTo.or(dateFrom), mealTicket.orNull(), file.orNull());

		AbsenceInsertResponseList airl = new AbsenceInsertResponseList();

		if(dateTo.isPresent() && dateFrom.isAfter(dateTo.get())){
			airl.getWarnings().add(String.format("La data di inizio delle ferie (%s) è successiva alla data di fine (%s)", dateFrom, dateTo));
		}

		List<Absence> absenceTypeAlreadyExisting = absenceTypeAlreadyExist(
				person, dateFrom, dateTo.or(dateFrom), absenceType);
		if (absenceTypeAlreadyExisting.size() > 0) {
			airl.getWarnings().add(AbsenceInsertResponse.CODICE_FERIE_GIA_PRESENTE);
			airl.getDatesInTrouble().addAll(Collections2.transform(absenceTypeAlreadyExisting, AbsenceToDate.INSTANCE));
		}

		List<Absence> allDayAbsenceAlreadyExisting = AbsenceDao.allDayAbsenceAlreadyExisting(person, dateFrom, dateTo);
		if (allDayAbsenceAlreadyExisting.size() > 0) {
			airl.getWarnings().add(AbsenceInsertResponse.CODICE_GIORNALIERO_GIA_PRESENTE);
			airl.getDatesInTrouble().addAll(Collections2.transform(allDayAbsenceAlreadyExisting, AbsenceToDate.INSTANCE));
		}

		if (airl.hasWarningOrDaysInTrouble()) {
			return airl;
		}

		LocalDate actualDate = dateFrom;

		while(!actualDate.isAfter(dateTo.or(dateFrom))){

			if (AbsenceTypeMapping.RIPOSO_COMPENSATIVO.is(absenceType)) {
				airl.add(handlerCompensatoryRest(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			if(AbsenceTypeMapping.FER.is(absenceType)){
				airl.add(handlerFER(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			if(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(absenceType) || 
					AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(absenceType) ||
					AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(absenceType)){
				airl.add(handler31_32_94(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
				
			}
			if(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.is(absenceType)){
				airl.add(handler37(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			//			TODO Inserire i codici di assenza necessari nell'AbsenceTypeMapping
			if((absenceType.code.startsWith("12") || absenceType.code.startsWith("13")) && absenceType.code.length() == 3){
				airl.add(handlerChildIllness(person, actualDate, absenceType, file));
				actualDate = actualDate.plusDays(1);
				continue;
			}
			if(absenceType.absenceTypeGroup != null){
				handlerAbsenceTypeGroup(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
				actualDate = actualDate.plusDays(1);
				continue;
			}
			
			airl.add(handlerGenericAbsenceType(person, actualDate, absenceType, file,mealTicket));

			actualDate = actualDate.plusDays(1);
		}
		
		if(airl.getAbsenceInReperibilityOrShift() > 0){
			sendEmail(person, airl);
		}					

		return airl;
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
	private static AbsenceInsertResponse insert(Person person, LocalDate date, 
			AbsenceType absenceType, Optional<Blob> file){

		Preconditions.checkNotNull(person);
		Preconditions.checkState(person.isPersistent());
		Preconditions.checkNotNull(date);
		Preconditions.checkNotNull(absenceType);
		Preconditions.checkState(absenceType.isPersistent());
		Preconditions.checkNotNull(file);

		AbsenceInsertResponse aim = new AbsenceInsertResponse(date,absenceType.code);

		//se non devo considerare festa ed è festa non inserisco l'assenza
		if(!absenceType.consideredWeekEnd && person.isHoliday(date)){
			aim.setHoliday(true);
			aim.setWarning(AbsenceInsertResponse.CODICE_NON_WEEKEND);
		}
		else{
			if(checkIfAbsenceInReperibilityOrInShift(person, date)){
				aim.setDayInReperibilityOrShift(true);				
			}

			PersonDay pd = null;
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(person, date, Optional.<LocalDate>absent(), false);
			//Costruisco se non esiste il person day
			if(pdList == null || pdList.isEmpty()){
				pd = new PersonDay(person, date);
				pd.create();
			}
			else{
				pd = pdList.listIterator().next();
			}

			//creo l'assenza e l'aggiungo
			Absence absence = new Absence();
			absence.absenceType = absenceType;
			absence.personDay = pd;
			absence.absenceFile = file.orNull();
			absence.save();

			aim.setAbsenceCode(absenceType.code);
			aim.setInsertSucceeded(true);

			Logger.info("Inserita nuova assenza %s per %s %s in data: %s", 
					absence.absenceType.code, absence.personDay.person.name,
					absence.personDay.person.surname, absence.personDay.date);

			pd.absences.add(absence);
			pd.populatePersonDay();

		}
		return aim;
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
	private static AbsenceInsertResponse handlerCompensatoryRest(Person person,
			LocalDate date, AbsenceType absenceType,Optional<Blob> file){

		Integer maxRecoveryDaysOneThree = Integer.parseInt(ConfYear.getFieldValue(
				ConfigurationFields.MaxRecoveryDays13.description, date.getYear(), person.office));
		//		TODO le assenze con codice 91 non sono sufficienti a coprire tutti i casi.
		//		Bisogna considerare anche eventuali inizializzazioni
		int alreadyUsed = AbsenceDao.getAbsenceByCodeInPeriod(
				Optional.fromNullable(person), Optional.fromNullable(absenceType.code),
				date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(),
				date, Optional.<JustifiedTimeAtWork>absent(), false, false).size();

		AbsenceInsertResponse aim = new AbsenceInsertResponse(date,absenceType.code);
		// 			verifica se ha esaurito il bonus per l'anno
		if(person.qualification.qualification > 0 && 
				person.qualification.qualification < 4 && 
				alreadyUsed >= maxRecoveryDaysOneThree){
			//			TODO	questo è il caso semplice,c'è da considerare anche eventuali cambi di contratto,
			//					assenze richieste per gennaio con residui dell'anno precedente sufficienti etc..
			aim.setWarning(String.format(AbsenceInsertResponse.RIPOSI_COMPENSATIVI_ESAURITI +
					" - Usati %s", alreadyUsed));
			return aim;
		}
		//Controllo del residuo
		if(AbsenceManager.canTakeCompensatoryRest(person, date)){
			return insert(person, date, absenceType, file);
		}

		aim.setWarning(AbsenceInsertResponse.MONTE_ORE_INSUFFICIENTE);
		return aim;
	}

	/**
	 * metodo che invia la mail contenente i giorni in cui ci sono inserimenti di assenza in turno o reperibilità
	 * @param person
	 * @param cai
	 * @throws EmailException
	 */
	private static void sendEmail(Person person, AbsenceInsertResponseList airl) {
		MultiPartEmail email = new MultiPartEmail();
		
		try {
			email.addTo(person.email);
			//Da attivare, commentando la riga precedente, per fare i test così da evitare di inviare mail a caso ai dipendenti...
			//email.addTo("dario.tagliaferri@iit.cnr.it");
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
	private static AbsenceInsertResponse handler31_32_94(Person person,
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
		AbsenceInsertResponse aim = new AbsenceInsertResponse(date,absenceType.code);
		aim.setWarning(AbsenceInsertResponse.NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO);
		return aim;
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
	private static AbsenceInsertResponse handler37(Person person,
			LocalDate date, AbsenceType absenceType,Optional<Blob> file){

		AbsenceInsertResponse aim = new AbsenceInsertResponse(date,absenceType.code);
//  FIXME Verificare i controlli d'inserimento
		if(date.getYear() == LocalDate.now().getYear()){

			int remaining37 = VacationsRecap.remainingPastVacationsAs37(date.getYear(), person);
			if(remaining37 > 0){
				return insert(person, date,absenceType, file);
			}
		}

		aim.setWarning(AbsenceInsertResponse.NESSUN_CODICE_FERIE_ANNO_PRECEDENTE_37);
		return aim;
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
	private static void handlerAbsenceTypeGroup(Person person,LocalDate dateFrom,
			LocalDate dateTo, AbsenceType absenceType, Blob file){

		LocalDate actualDate = dateFrom;
		while(actualDate.isBefore(dateTo) || actualDate.isEqual(dateTo)){

			CheckMessage checkMessage = PersonUtility.checkAbsenceGroup(absenceType, person, actualDate);
			if(checkMessage.check == false){
				//				flash.error("Impossibile inserire il codice %s per %s %s. "+checkMessage.message, absenceType.code, person.name, person.surname);
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}

			PersonDay pd = PersonDayDao.getPersonDayInPeriod(person, actualDate, Optional.<LocalDate>absent(), false).size() > 0 ? PersonDayDao.getPersonDayInPeriod(person, actualDate, Optional.<LocalDate>absent(), false).get(0) : null;
			//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
				pd = new PersonDay(person, actualDate);
				pd.populatePersonDay();
				pd.save();
			}
			if(checkMessage.check == true && checkMessage.absenceType ==  null){

				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;

				if (file != null && file.exists()) {
					absence.absenceFile = file;
				}

				absence.save();

				pd.populatePersonDay();
				pd.save();
				pd.updatePersonDaysInMonth();

			}
			else if(checkMessage.check == true && checkMessage.absenceType != null){
				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;

				if (file != null && file.exists()) {
					absence.absenceFile = file;
				}

				absence.save();
				pd.absences.add(absence);
				Absence compAbsence = new Absence();
				compAbsence.absenceType = checkMessage.absenceType;
				compAbsence.personDay = pd;
				compAbsence.save();
				pd.absences.add(compAbsence);
				pd.save();
				pd.populatePersonDay();
				pd.updatePersonDaysInMonth();

			}
			actualDate = actualDate.plusDays(1);
		}

		//		flash.success("Aggiunto codice di assenza %s ", absenceType.code);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());

	}

	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param absenceType
	 * @throws EmailException 
	 */
	private static AbsenceInsertResponse handlerChildIllness(Person person,LocalDate date,
				AbsenceType absenceType, Optional<Blob> file){
		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		//TODO: se il dipendente ha più di 9 figli! non funziona dal 10° in poi

		Boolean esito = PersonUtility.canTakePermissionIllnessChild(person, date, absenceType);
		
		if(esito){
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
		AbsenceInsertResponse aim = new AbsenceInsertResponse(date,absenceType.code);
		aim.setWarning(AbsenceInsertResponse.CODICI_MALATTIA_FIGLI_NON_DISPONIBILE);
		return aim;
	}

	/**
	 * Gestisce l'inserimento dei codici FER, 94-31-32 nell'ordine. Fino ad esaurimento.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @throws EmailException 
	 */
	private static AbsenceInsertResponse handlerFER(Person person,LocalDate date,
			AbsenceType absenceType, Optional<Blob> file){

		AbsenceType wichFer = AbsenceManager.whichVacationCode(person, date);

		//FER esauriti
		if(wichFer==null){
			AbsenceInsertResponse aim = new AbsenceInsertResponse(date,absenceType.code);
			aim.setWarning(AbsenceInsertResponse.NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO);
			return aim;
		}
		return insert(person, date, wichFer, file);
	}

	private static AbsenceInsertResponse handlerGenericAbsenceType(Person person,LocalDate date,
			AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket){

		AbsenceInsertResponse aim = insert(person, date, absenceType, file);
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
	private static void checkMealTicket(LocalDate date, Person person, String mealTicket, AbsenceType abt){

		PersonDay pd = PersonDayDao.getPersonDayInPeriod(person, date, Optional.<LocalDate>absent(), false).get(0);

		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		if(pd == null)
			pd = new PersonDay(person, date);
		if(abt==null || !abt.code.equals("92"))
		{
			pd.isTicketForcedByAdmin = false;	//una assenza diversa da 92 ha per forza campo calcolato
			pd.populatePersonDay();
			return;
		}
		if(mealTicket!= null && mealTicket.equals("si")){
			pd.isTicketForcedByAdmin = true;
			pd.isTicketAvailable = true;
			pd.populatePersonDay();
		}
		if(mealTicket!= null && mealTicket.equals("no")){
			pd.isTicketForcedByAdmin = true;
			pd.isTicketAvailable = false;
			pd.populatePersonDay();
		}

		if(mealTicket!= null && mealTicket.equals("calcolato")){
			pd.isTicketForcedByAdmin = false;
			pd.populatePersonDay();
		}
	}

	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 */
	public static int removeAbsencesInPeriod(Person person, LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType)
	{
		LocalDate today = new LocalDate();
		LocalDate actualDate = dateFrom;
		int deleted = 0;
		while(!actualDate.isAfter(dateTo))
		{

			PersonDay pd = null;
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(person, actualDate, Optional.<LocalDate>absent(), false);
			//Costruisco se non esiste il person day
			if(pdList.size() == 0){
				actualDate = actualDate.plusDays(1);
				continue;
			}
			else{
				pd = pdList.get(0);
			}

			//	PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			//			if(pd == null)
			//			{
			//				actualDate = actualDate.plusDays(1);
			//				continue;
			//			}
			List<Absence> absenceList = AbsenceDao.getAbsenceInDay(Optional.fromNullable(person), actualDate, Optional.<LocalDate>absent(), false);
			//			List<Absence> absenceList = Absence.find("Select ab from Absence ab, PersonDay pd where ab.personDay = pd and pd.person = ? and pd.date = ?", 
			//					person, actualDate).fetch();
			for(Absence absence : absenceList)
			{
				if(absence.absenceType.code.equals(absenceType.code))
				{
					absence.delete();
					pd.absences.remove(absence);
					pd.isTicketForcedByAdmin = false;
					pd.populatePersonDay();
					deleted++;
					Logger.info("Rimossa assenza del %s per %s %s", actualDate, person.name, person.surname);
				}
			}
			if(pd.date.isAfter(today) && pd.absences.isEmpty() && pd.absences.isEmpty()){
				pd.delete();
			}
			actualDate = actualDate.plusDays(1);
		}
		return deleted;
	}


}
