package manager;

import it.cnr.iit.epas.CheckAbsenceInsert;
import it.cnr.iit.epas.CheckMessage;
import it.cnr.iit.epas.PersonUtility;

import java.util.List;

import javax.persistence.Query;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Absence;
import models.AbsenceType;
import models.ConfYear;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.enumerate.ConfigurationFields;
import models.enumerate.JustifiedTimeAtWork;
import models.rendering.VacationsRecap;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

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

	/**
	 * Il primo codice utilizzabile per l'anno selezionato come assenza nel seguente ordine 31,32,94
	 * @param person
	 * @param actualDate
	 * @return
	 */
	public static AbsenceType whichVacationCode(Person person, LocalDate date){

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
	public static AbsenceType takeAnother32(Person person, LocalDate date) {
		
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}
		
		if(vr.vacationDaysCurrentYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "32").first();
		
		return null;
		
	}
	
	/**
	 * Verifica che la persona alla data possa prendere un giorno di ferie codice 31.
	 * @param person
	 * @param date
	 * @return l'absenceType 31 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	public static AbsenceType takeAnother31(Person person, LocalDate date) {
		
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}
		
		if(vr.vacationDaysLastYearNotYetUsed > 0)
			return AbsenceType.find("byCode", "31").first();

		return null;
	}
	
	/**
	 * Verifica che la persona alla data possa prendere un giorno di permesso codice 94.
	 * @param person
	 * @param date
	 * @return l'absenceType 94 in caso affermativo. Null in caso di esaurimento bonus.
	 * 
	 */
	public static AbsenceType takeAnother94(Person person, LocalDate date) {
		
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vr = null;
		try { 
			vr = new VacationsRecap(person, date.getYear(), contract, date, true);
		} catch(IllegalStateException e) {
			return null;
		}
		
		if(vr.persmissionNotYetUsed > 0)
			return AbsenceType.find("byCode", "94").first();

		return null;
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
		if( dateToCheck.getMonthOfYear() == LocalDate.now().getMonthOfYear() + 1) 
		{
			dateToCheck = LocalDate.now();
		}
		//Caso particolare dicembre - gennaio
		else if( dateToCheck.getYear() == LocalDate.now().getYear() + 1 
				&& dateToCheck.getMonthOfYear() == 1 && LocalDate.now().getMonthOfYear() == 12) 
		{
			
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
	
	public static void insertAbsence(Person person, LocalDate dateFrom,Optional<LocalDate> dateTo, 
			AbsenceType absenceType, Optional<Blob> file, Optional<String> mealTicket){
		
		if(absenceTypeAlreadyExist(person, dateFrom, dateTo.or(dateFrom), absenceType)){
//			TODO Sostituire il messaggio nello scope con un eccezzione
//			flash.error("Il codice di assenza %s è già presente in almeno uno dei giorni in cui lo si voleva inserire. Controllare", absenceType.code);
		}
		
		if(allDayAbsenceAlreadyExist(person, dateFrom, dateTo.or(dateFrom)) && absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
//			flash.error("Non si possono inserire per lo stesso giorno due codici di assenza giornaliera. Operazione annullata.");
		}
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		if(absenceType.code.equals("91")){
			int taken = handlerCompensatoryRest(person, dateFrom, dateTo.or(dateFrom), absenceType, file);
//			if(taken==0)
//				flash.error("Non e' stato possibile inserire alcun riposo compensativo (bonus esaurito o residuo insufficiente)");
//			else
//				flash.success("Inseriti %s riposi compensativi per la persona", taken);
			return;
		}
		
		if(absenceType.code.equals("FER")){
			handlerFER(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
			return;
		}
		if(absenceType.code.equals("32") || absenceType.code.equals("31") || absenceType.code.equals("94"))
		{
			handler32_31_94(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
		}
		
		if(absenceType.code.equals("37")){
			handler37(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
			return;
		}	

		if((absenceType.code.startsWith("12") || absenceType.code.startsWith("13")) && absenceType.code.length() == 3){
			handlerChildIllness(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
			return;
		}
		
		if(absenceType.code.equals("92")){
			handlerGenericAbsenceType(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull(), mealTicket.orNull());
			return;
		}

		if(absenceType.absenceTypeGroup != null){
			handlerAbsenceTypeGroup(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
			return;
		}
		
		if(absenceType.consideredWeekEnd){
			//FIXME disastroso, considereWeekEnd true cattura anche codici diversi da illness e discharge. 
			//Mettere condizione diversa.
			handlerIllnessOrDischarge(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull());
			return;
		}
			
		handlerGenericAbsenceType(person, dateFrom, dateTo.or(dateFrom), absenceType, file.orNull(), mealTicket.orNull());
		
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo.or(dateFrom));
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	}
	
	/**
	 * Controlla che nell'intervallo passato in args non esistano già assenze per quel tipo
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @return
	 */
	private static boolean absenceTypeAlreadyExist(Person person,LocalDate dateFrom,
			LocalDate dateTo, AbsenceType absenceType){
		
		return AbsenceDao.findByPersonAndDate
				(person, dateFrom, Optional.fromNullable(dateTo),
						Optional.fromNullable(absenceType)).count > 0;
	}
	
	/**
	 * Controlla che nell'intervallo passato in args non esista gia' una assenza giornaliera
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @return
	 */
	private static boolean allDayAbsenceAlreadyExist(Person person,LocalDate dateFrom, LocalDate dateTo){
		
		List<Absence> absenceList = AbsenceDao.findByPersonAndDate
				(person, dateFrom, Optional.fromNullable(dateTo),
						Optional.<AbsenceType>absent()).list();
		
		return	!Collections2.filter(absenceList, 
	    		new Predicate<Absence>() {
	    	    @Override
	    	    public boolean apply(Absence ab) {
	    	        return ab.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay;
	    	    }
	    	}).isEmpty();
	}
	
	/**
	 * Gestisce l'inserimento dei codici 91 (1 o più consecutivi)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @throws EmailException 
	 */
	private static int handlerCompensatoryRest(Person person,LocalDate dateFrom, 
			LocalDate dateTo, AbsenceType absenceType,Optional<Blob> file){
		
		LocalDate actualDate = dateFrom;
		Integer maxRecoveryDaysOneThree = Integer.parseInt(ConfYear.getFieldValue(
				ConfigurationFields.MaxRecoveryDays13.description, actualDate.getYear(), person.office));
		int alreadyUsed = AbsenceDao.getAbsenceByCodeInPeriod(
				Optional.fromNullable(person), Optional.fromNullable(absenceType.code),
				dateFrom.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(),
				dateTo, Optional.<JustifiedTimeAtWork>absent(), false, false).size();
		int taken = 0;
		
		while(!actualDate.isAfter(dateTo)){
			//verifica se ha esaurito il bonus per l'anno
			if(person.qualification.qualification > 0 && person.qualification.qualification < 4){
				Logger.debug("Il numero di assenze con codice %s fino a oggi è %d", absenceType.code, alreadyUsed);
				if(alreadyUsed >= maxRecoveryDaysOneThree){
					actualDate = actualDate.plusDays(1);
					continue;
				}
			}
			//Controllo del residuo
			if(!AbsenceManager.canTakeCompensatoryRest(person, actualDate)){
				actualDate = actualDate.plusDays(1);
				continue;
			}
			
			insertAbsencesInPeriod(person, actualDate, Optional.<LocalDate>absent(), absenceType, file);
			taken++;
			alreadyUsed++;
			actualDate = actualDate.plusDays(1);
		}
		return taken;
	}
	
	/**
	 * Inserisce l'assenza absenceType nel person day della persona nel periodo indicato.
	 *  Se dateFrom = dateTo inserisce nel giorno singolo.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @return	il numero di assenze effettivamente inserite nel periodo passato come argomento.
	 */
	private static int insertAbsencesInPeriod(Person person, LocalDate dateFrom, 
			Optional<LocalDate> dateTo, AbsenceType absenceType, Optional<Blob> file){
		
		CheckAbsenceInsert cai = new CheckAbsenceInsert(0,null, false, 0);
		LocalDate actualDate = dateFrom;
		
		while(!actualDate.isAfter(dateTo.or(dateFrom))){
			//se non devo considerare festa ed è festa vado oltre
			if(!absenceType.consideredWeekEnd && person.isHoliday(actualDate)){
				actualDate = actualDate.plusDays(1);
				continue;
			}			
			
//	FIXME	   Questo controllo non dovrebbe essere fatto nel metodo handler32_31_94 ??????
			
//			//Controllo il caso di inserimento di codice 31: verifico che sia valido il periodo in cui voglio inserirlo
//			if(absenceType.code.equals("31") && !absenceType.code.equals(AbsenceManager.whichVacationCode(person, actualDate).code)){
//				flash.error("Si prova a inserire un codice di assenza (%s , %s) che non è prendibile per il giorno %s", 
//						absenceType.code, absenceType.description, actualDate);
//				
//				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
//			}
			
			PersonDay pd = null;
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(person, actualDate, Optional.<LocalDate>absent(), false);
			//Costruisco se non esiste il person day
			if(pdList == null || pdList.isEmpty()){
				pd = new PersonDay(person, actualDate);
				pd.create();
			}
			else{
				pd = pdList.iterator().next();
			}

			if(checkIfAbsenceInReperibilityOrInShift(person, actualDate)){
				cai.insertInShiftOrReperibility = true;
				cai.howManyAbsenceInReperibilityOrShift++;
				cai.dateInTrouble.add(actualDate);				
			}
			
			//creo l'assenza e l'aggiungo
			Absence absence = new Absence();
			absence.absenceType = absenceType;
			absence.personDay = pd;
			absence.absenceFile = file.orNull();
			absence.save();
			
			Logger.info("Inserita nuova assenza %s per %s %s in data: %s", 
					absence.absenceType.code, absence.personDay.person.name,
					absence.personDay.person.surname, absence.personDay.date);
			
			pd.absences.add(absence);
			pd.populatePersonDay();
			
			cai.totalAbsenceInsert++;
			
			actualDate = actualDate.plusDays(1);
		}
		//controllo che ci siano date in cui l'assenza sovrascrive una reperibilità o un turno e nel caso invio la mail
		if(cai.dateInTrouble.size() > 0) {
			try {
				sendEmail(person, cai);
			} catch (EmailException e) {
				// TODO Segnalare questo evento in qualche modo
				e.printStackTrace();
			}
		}
		return cai.totalAbsenceInsert;
	}
	
	/**
	 * metodo che invia la mail contenente i giorni in cui ci sono inserimenti di assenza in turno o reperibilità
	 * @param person
	 * @param cai
	 * @throws EmailException
	 */
	private static void sendEmail(Person person, CheckAbsenceInsert cai) throws EmailException{
		MultiPartEmail email = new MultiPartEmail();

		email.addTo(person.email);
		//Da attivare, commentando la riga precedente, per fare i test così da evitare di inviare mail a caso ai dipendenti...
		//email.addTo("dario.tagliaferri@iit.cnr.it");
		email.setFrom("epas@iit.cnr.it");
		
		email.setSubject("Segnalazione inserimento assenza in giorno con reperibilità/turno");
		String date = "";
		for(LocalDate data : cai.dateInTrouble){
			date = date+data+' ';
		}
		email.setMsg("E' stato richiesto l'inserimento di una assenza per il giorno "+date+ 
				" per il quale risulta una reperibilità o un turno attivi. "+'\n'+
				"Controllare tramite la segreteria del personale."+'\n'+
				'\n'+
				"Servizio ePas");
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
	 * Gestisce l'inserimento esplicito dei codici 32, 31 e 94.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 */
	private static void handler32_31_94(Person person,LocalDate dateFrom, 
			LocalDate dateTo, AbsenceType absenceType, Blob file){
		
		Preconditions.checkArgument(absenceType.code.equals("32") || absenceType.code.equals("31") || absenceType.code.equals("94"));
		
		LocalDate actualDate = dateFrom;

		//inserimento
		int taken = 0;
		
		actualDate = dateFrom;
		while(!actualDate.isAfter(dateTo))
		{
				
			AbsenceType anotherAbsence = absenceType;
			
			if(absenceType.code.equals("32")) {
				anotherAbsence = AbsenceManager.takeAnother32(person, actualDate);
			}
			if(absenceType.code.equals("31")) {
				anotherAbsence = AbsenceManager.takeAnother31(person, actualDate);
			}
			if(absenceType.code.equals("94")) {
				anotherAbsence = AbsenceManager.takeAnother94(person, actualDate);
			}
			
			//Esaurito
			if(anotherAbsence == null)
			{
				if(taken == 0)
				{
//					flash.error("Il dipendente %s %s ha esaurito tutti i codici %s", person.name, person.surname, absenceType.code);
					Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
				}
				
//				flash.error("Aggiunti %s codici assenza %s da %s a %s. In data %s il dipendente ha esaurito tutti i codici %s a disposizione.",
//						taken, absenceType.code, dateFrom, actualDate.minusDays(1), actualDate, absenceType.code);

				PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}

			insertAbsencesInPeriod(person, actualDate, Optional.<LocalDate>absent(), 
					anotherAbsence, Optional.fromNullable(file));
			taken++;
			actualDate = actualDate.plusDays(1);
			
		}
		actualDate = actualDate.minusDays(1);

		
		if(taken==1)
//			flash.success("Aggiunto codice assenza %s per il giorno %s", absenceType.code, actualDate);
		if(taken>1)
//			flash.success("Aggiunti %s codici assenza %s da %s a %s.", taken, absenceType.code, dateFrom, dateTo);
		
		PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
		
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
	private static void handler37(Person person,LocalDate dateFrom, LocalDate dateTo,
			AbsenceType absenceType, Blob file){
		if(dateFrom.getYear() != dateTo.getYear()){
//			flash.error("I recuperi ferie anno precedente possono essere assegnati solo per l'anno corrente");
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}

		int remaining37 = VacationsRecap.remainingPastVacationsAs37(dateFrom.getYear(), person);
		if(remaining37 == 0){
//			flash.error("La persona selezionata non dispone di ulteriori giorni ferie anno precedente");
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}
		
		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo) && taken<=remaining37)
		{

			insertAbsencesInPeriod(person, actualDate, Optional.<LocalDate>absent(), 
					absenceType, Optional.fromNullable(file));
			taken++;
			actualDate = actualDate.plusDays(1);
		}

//		if(taken > 0)
//			flash.success("Inseriti %s codice 37 per la persona", taken);
//		else
//			flash.error("Impossibile inserire codici 37 per la persona");
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
		
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
	private static void handlerChildIllness(Person person, LocalDate dateFrom, 
			LocalDate dateTo, AbsenceType absenceType, Blob file){
		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		//TODO: se il dipendente ha più di 9 figli! non funziona dal 10° in poi
		
		Boolean esito = PersonUtility.canTakePermissionIllnessChild(person, dateFrom, absenceType);

		if(esito==null)
		{
//			flash.error("ATTENZIONE! In anagrafica la persona selezionata non ha il numero di figli sufficienti per valutare l'assegnazione del codice di assenza nel periodo selezionato. "
//					+ "Accertarsi che la persona disponga dei privilegi per usufruire dal codice e nel caso rimuovere le assenze inserite.");
		}
		else if(!esito)
		{
//			flash.error(String.format("Il dipendente %s %s non può prendere il codice d'assenza %s poichè ha già usufruito del numero" +
//					" massimo di giorni di assenza per quel codice o non ha figli che possono usufruire di quel codice", person.name, person.surname, absenceType.code));
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
			return;
		}

		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo)){

			insertAbsencesInPeriod(person, actualDate, Optional.<LocalDate>absent(), 
					absenceType, Optional.fromNullable(file));
			actualDate = actualDate.plusDays(1);
		}
//		if(taken > 0)
//			flash.success("Inseriti %s codici assenza per la persona", taken);
//		else
//			flash.error("Impossibile inserire codici di assenza per malattia figli");
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	
	}
	
	/**
	 * Gestisce l'inserimento dei codici FER, 94-31-32 nell'ordine. Fino ad esaurimento.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @throws EmailException 
	 */
	private static void handlerFER(Person person,LocalDate dateFrom, LocalDate dateTo,
			AbsenceType absenceType, Blob file){
		LocalDate actualDate = dateFrom;

		//inserimento
		int taken = 0;
		
		actualDate = dateFrom;
		while(!actualDate.isAfter(dateTo))
		{
						
			AbsenceType wichFer = AbsenceManager.whichVacationCode(person, actualDate);
			
			//FER esauriti
			if(wichFer==null)
			{
				if(taken==0)
				{
//					flash.error("Il dipendente %s %s ha esaurito tutti i codici FER", person.name, person.surname);
					Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
				}
//				flash.error("Aggiunti %s codici assenza FER da %s a %s. In data %s il dipendente ha esaurito tutti i codici FER a disposizione.", taken, dateFrom, actualDate.minusDays(1), actualDate);
				PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}

			insertAbsencesInPeriod(person, actualDate, Optional.<LocalDate>absent(), wichFer, Optional.fromNullable(file));
			taken++;
			actualDate = actualDate.plusDays(1);
			
		}
		actualDate = actualDate.minusDays(1);

		
//		if(taken==1)
//			flash.success("Aggiunto codice assenza FER per il giorno %s", actualDate);
//		if(taken>1)
//			flash.success("Aggiunti %s codici assenza FER da %s a %s.", taken, dateFrom, dateTo);
		
		PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());

	}
	
	private static void handlerGenericAbsenceType(Person person,LocalDate dateFrom,
			LocalDate dateTo, AbsenceType absenceType, Blob file, String mealTicket){
		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo)) {
			
			insertAbsencesInPeriod(person, actualDate, Optional.<LocalDate>absent(), absenceType, Optional.fromNullable(file));
			taken++;
			checkMealTicket(actualDate, person, mealTicket, absenceType);


			actualDate = actualDate.plusDays(1);
		}

//		if(taken > 0)
//			flash.success("Inseriti %s codici assenza per la persona", taken);
//		else
//			flash.error("Impossibile inserire il codice di assenza %s", absenceType.code);
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	}
	
	/**
	 * Gestore assenze di malattia e congedo (in cui considerer week end è true)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @throws EmailException 
	 */
	private static void handlerIllnessOrDischarge(Person person,LocalDate dateFrom,
			LocalDate dateTo, AbsenceType absenceType,Blob file){
		int taken = insertAbsencesInPeriod(person, dateFrom,Optional.fromNullable(dateTo) , absenceType, Optional.fromNullable(file));
//		flash.success("Inseriti %s codici assenza per la persona", taken);
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
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
