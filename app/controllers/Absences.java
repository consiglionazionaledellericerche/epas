package controllers;

import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Configuration;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonTags;
import models.Qualification;
import models.Stamping;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;

import org.hibernate.envers.entities.mapper.relation.lazy.proxy.SetProxy;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Absences extends Controller{

	private static List<AbsenceType> getFrequentAbsenceTypes(){
		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
				"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();

	}

	private static List<AbsenceType> getAllAbsenceTypes(LocalDate date){

		return AbsenceType.find("Select abt from AbsenceType abt where abt.validTo > ? order by code", date).fetch();
	}

	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absences(Integer year, Integer month) {
		Person person = Security.getPerson();

		PersonMonth personMonth = PersonMonth.byPersonAndYearAndMonth(person, year, month);

		if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}

		render(personMonth);

	}

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absenceInMonth(Long personId, String absenceCode, int year, int month){
		List<LocalDate> dateAbsences = new ArrayList<LocalDate>();
		Person person = Person.findById(personId);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, new LocalDate(year,month,1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences != null){
				for(Absence abs : pd.absences){
					if(abs.absenceType.code.equals(absenceCode))
						dateAbsences.add(pd.date);
				}
			}
		}
		render(dateAbsences, absenceCode);
	}

	/**
	 * questa è una funzione solo per admin, quindi va messa con il check administrator
	 */
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void manageAbsenceCode(){
		List<AbsenceType> absenceList = AbsenceType.find("Select abt from AbsenceType abt order by abt.code").fetch();

		render(absenceList);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void absenceCodeList(){
		List<AbsenceType> absenceList = AbsenceType.findAll();
		render(absenceList);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insertAbsenceCode(){
		AbsenceType abt = new AbsenceType();
		AbsenceTypeGroup  abtg = new AbsenceTypeGroup();
		List<Qualification> qualificationList = Qualification.findAll();
		List<AbsenceTypeGroup> abtList = AbsenceTypeGroup.findAll();
		List<JustifiedTimeAtWork> justifiedTimeAtWorkList = new ArrayList<JustifiedTimeAtWork>();
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.AllDay);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.EightHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.FiveHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.FourHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.HalfDay);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.Nothing);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.OneHour);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.SevenHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.SixHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.ThreeHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.TimeToComplete);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.TwoHours);
		List<AccumulationType> accumulationTypeList = new ArrayList<AccumulationType>();
		accumulationTypeList.add(AccumulationType.always);
		accumulationTypeList.add(AccumulationType.monthly);
		accumulationTypeList.add(AccumulationType.no);
		accumulationTypeList.add(AccumulationType.yearly);

		List<AccumulationBehaviour> accumulationBehaviourList = new ArrayList<AccumulationBehaviour>();
		accumulationBehaviourList.add(AccumulationBehaviour.noMoreAbsencesAccepted);
		accumulationBehaviourList.add(AccumulationBehaviour.nothing);
		accumulationBehaviourList.add(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation);

		render(abt, abtg, qualificationList, abtList, justifiedTimeAtWorkList, accumulationTypeList, accumulationBehaviourList);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void saveAbsenceCode(){
		AbsenceType abt = new AbsenceType();
		AbsenceTypeGroup abtg = null;
		abt.code = params.get("codice");
		if(params.get("codiceAttestati") != null)
			abt.certificateCode = params.get("codiceAttestati");
		else
			abt.certificateCode = null;
		abt.description = params.get("descrizione");
		abt.ignoreStamping = params.get("ignoraTimbrature", Boolean.class);
		abt.internalUse = params.get("usoInterno", Boolean.class);
		abt.mealTicketCalculation = params.get("calcoloBuono", Boolean.class);
		if(params.get("inizioValidita", Date.class) != null){
			Date validFrom = params.get("inizioValidita", Date.class);
			abt.validFrom = new LocalDate(validFrom);
		}
		else
			abt.validFrom = null;
		if(params.get("fineValidita", Date.class) != null){
			Date validTo = params.get("fineValidita", Date.class);
			abt.validTo = new LocalDate(validTo);
		}
		else
			abt.validTo = null;

		abt.justifiedTimeAtWork = params.get("jwt", JustifiedTimeAtWork.class);
		abt.multipleUse = params.get("usoMultiplo", Boolean.class);

		if(params.get("livello1", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 1).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello2", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 2).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello3", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 3).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello4", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 4).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello5", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 5).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello6", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 6).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello7", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 7).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello8", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 8).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello9", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 9).first();
			abt.qualifications.add(qual);
		}

		if(!params.get("gruppo").equals("")){
			abtg = new AbsenceTypeGroup();
			abtg.accumulationBehaviour = params.get("accBehaviour", AccumulationBehaviour.class);
			abtg.accumulationType = params.get("accType", AccumulationType.class);
			abtg.label = params.get("gruppo");
			abtg.limitInMinute = params.get("limiteAccumulo", Integer.class);
			abtg.minutesExcess = params.get("minutiEccesso", Boolean.class);
			abtg.replacingAbsenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", params.get("codicePerSostituzione")).first();
			abtg.save();
		}
		abt.absenceTypeGroup = abtg;
		abt.save();
		flash.success(
				String.format("Inserita nuova assenza con codice %s", abt.code));
		Application.indexAdmin();
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void discard(){
		manageAbsenceCode();
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void create(@Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day) {
		Logger.debug("Insert absence called for personId=%d, year=%d, month=%d, day=%d", personId, year, month, day);
		List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
		MainMenu mainMenu = new MainMenu(year,month,day);
		List<AbsenceType> allCodes = getAllAbsenceTypes(new LocalDate(year,month,day));
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate date = new LocalDate(year, month, day);
		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes, mainMenu);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insert(@Required Long personId, @Required Integer yearFrom, 
			@Required Integer monthFrom, @Required Integer dayFrom, @Required String absenceCode, Integer annoFine, Integer meseFine, Integer giornoFine){

		Person person = Person.em().getReference(Person.class, personId);
		LocalDate dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
		LocalDate dateTo = new LocalDate(annoFine, meseFine, giornoFine);
		Logger.debug("La data fine è: %s", dateTo);
		AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
		Logger.trace("Controllo la presenza dell'absenceType %s richiesto per l'assenza del giorno %s per personId = %s ", absenceType, dateFrom, personId);
		if (absenceType == null) {
			validation.keep();
			params.flash();

			flash.error("Il codice di assenza %s non esiste", params.get("absenceCode"));
			Logger.info("E' stato richiesto l'inserimento del codice di assenza %s per l'assenza del giorno %s per personId = %d. Il codice NON esiste. Se si tratta di un codice di assenza per malattia figlio NUOVO, inserire il nuovo codice nella lista e riprovare ad assegnarlo.", absenceType, dateFrom, personId);
			create(personId, yearFrom, monthFrom, dayFrom);
			render("@save");
		}

		Logger.debug("Richiesto inserimento della assenza codice = %s della persona %s, dataInizio = %s", absenceCode, person, dateFrom);

		// è il caso in cui si inserisce lo stesso codice di assenza per più giorni
		
		if(dateTo.isAfter(dateFrom)){

			LocalDate dataInizioAssenze = dateFrom;
			while(dataInizioAssenze.isBefore(dateTo)){

				List<Absence> absenceList = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?", 
						person, dataInizioAssenze).fetch();
				for(Absence abs : absenceList){
					if(abs.absenceType.equals(absenceType)){
						flash.error("Il codice di assenza %s è già presente in almeno uno dei giorni in cui lo si voleva inserire. Controllare", absenceType.code);
						create(personId, yearFrom, monthFrom, dayFrom);
						render("@save");
					}
											
				}
				dataInizioAssenze = dataInizioAssenze.plusDays(1);
			}		
			
		}
		else{
			
			List<Absence> existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?" +
					" and a.absenceType = ?", person, dateFrom, absenceType).fetch();
			if(existingAbsence.size() > 0){
				if((existingAbsence.get(0).absenceType.equals(absenceType) || existingAbsence.get(1).absenceType.equals(absenceType))){
					validation.keep();
					params.flash();
					flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(dateFrom));
					create(personId, yearFrom, monthFrom, dayFrom);
					render("@save");
				}
			}
			
		}
				
		Absence abs = Absence.find("Select abs from Absence abs where abs.personDay.person = ? and abs.personDay.date = ?", person, dateFrom).first();
		if(abs != null && abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay && 
				absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
			flash.error("Non si possono inserire per lo stesso giorno due codici di assenza giornaliera");
			render("@save");
		}

		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		//TODO: se il dipendente ha più di 9 figli! non funziona dal 10° in poi
		if((absenceType.code.startsWith("12") || absenceType.code.startsWith("13")) && absenceType.code.length() == 3){
			if(!PersonUtility.canTakePermissionIllnessChild(person, dateFrom, absenceType)){
				/**
				 * non può usufruire del permesso
				 */
				flash.error(String.format("Il dipendente %s %s non può prendere il codice d'assenza %s poichè ha già usufruito del numero" +
						" massimo di giorni di assenza per quel codice", person.name, person.surname, absenceType.code));
				render("@save");
				return;

			}
		}
		
		/**
		 * in questo pezzo si controlla il poter inserire i codici per le assenze dovute a malattie o ricoveri anche nei giorni festivi.
		 * Da risistemare quando verrà cambiato il database anche in produzione e allora bisognerà controllare che il codice d'assenza inserito 
		 * abbia il campo "considered_week_end" = true
		 */
		if(absenceType.consideredWeekEnd){
			if(dateTo.isBefore(dateFrom) || dateTo.isEqual(dateFrom)){
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, new LocalDate(yearFrom, monthFrom, dayFrom)).first();
				if(pd == null){
					pd = new PersonDay(person, dateFrom);
					pd.create();
				}
				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;
				absence.save();
				flash.success("Inserito il codice d'assenza %s nel giorno %s", absenceType.code, pd.date);
				render("@save");
			}
			else{
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
						person, dateFrom, dateTo).fetch();
				if(pdList.size() != 0){
					for(PersonDay pd : pdList){
						Absence absence = new Absence();
						absence.absenceType = absenceType;
						absence.personDay = pd;
						absence.save();
						pd.absences.add(absence);
						pd.save();
						pd.populatePersonDay();
						
					}
				}
				else{
					while(!dateFrom.isAfter(dateTo)){
						PersonDay pd = new PersonDay(person, dateFrom);
						pd.create();
						Absence absence = new Absence();
						absence.absenceType = absenceType;
						absence.personDay = pd;
						absence.save();
						pd.absences.add(absence);
						pd.merge();
						
						pd.populatePersonDay();
						pd.save();
						
						dateFrom = dateFrom.plusDays(1);
					}
				}
				flash.success("Inserito codice d'assenza %s per il periodo richiesto", absenceType.code);
				render("@save");
			}
		}

		/**
		 * qui controllare il fatto che l'utente da tastiera possa aver inserito il codice "FER" e con quello, quindi, andare a cercare di 
		 * inserire il giusto codice di assenza per ferie in base a quante ferie potevano essere rimaste dall'anno precedente, eventualmente passare
		 * da quelle dell'anno in corso o ancora dai permessi legge...ok ma qual'è l'ordine? :-)
		 */
		//TODO: sarebbe meglio utilizzare degli ENUM con il mapping tra codici del DB e codici più comuni
		if(absenceType.code.equals("FER")){
			//FIXME: perché il controllo successivo è fatto solo per il FER?
			if(PersonUtility.canPersonTakeAbsenceInShiftOrReperibility(person, new LocalDate(yearFrom,monthFrom,dayFrom))){
				Logger.debug("%s %s non è in turno o in reperibilità", person.name, person.surname);
				
				if(dateTo.isBefore(dateFrom) || dateTo.isEqual(dateFrom)){
					AbsenceType abt = PersonUtility.whichVacationCode(person, yearFrom, monthFrom, dayFrom);
					PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, new LocalDate(yearFrom, monthFrom, dayFrom)).first();
					if(pd == null){
						pd = new PersonDay(person, new LocalDate(yearFrom, monthFrom, dayFrom));
						pd.create();
					}
					Absence absence = new Absence();
					absence.absenceType = abt;
					absence.personDay = pd;
					absence.save();
					flash.success("Inserito il codice di assenza %s per il giorno %s", abt.code, pd.date);
					render("@save");
				}
				else{
					
					List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
							person, dateFrom, dateTo).fetch();
					
					if(pdList.size() != 0){
						for(PersonDay pd : pdList){
							AbsenceType abt = PersonUtility.whichVacationCode(person, yearFrom, monthFrom, dayFrom);
							Absence absence = new Absence();
							absence.absenceType = abt;
							absence.personDay = pd;
							absence.save();
							pd.save();
							pd.populatePersonDay();
							
						}
					}
					else{
						while(!dateFrom.isAfter(dateTo)){
							Logger.debug("Devo creare il personDay perchè il giorno è futuro rispetto a oggi");
							AbsenceType abt = PersonUtility.whichVacationCode(person, yearFrom, monthFrom, dayFrom);
							PersonDay pd = PersonUtility.createPersonDayFromDate(person, dateFrom);
							if(pd != null){
								Logger.debug("Nel controller absences vado a creare il personDay e a inserire l'assenza per %s %s nel giorno %s", 
										person.name, person.surname, dateFrom);
								pd.create();
								Absence absence = new Absence();
								absence.absenceType = abt;
								absence.personDay = pd;
								absence.save();
								pd.absences.add(absence);
								pd.merge();
								
								pd.populatePersonDay();
								pd.save();
								
							}
							dateFrom = dateFrom.plusDays(1);
						}
					}
//					int size = pdList.size();
//					Logger.debug("la dimensione della lista è: %d", size);
//					PersonDay pd = pdList.get(size-1);
//					if(pd.date.isBefore(new LocalDate(pd.date).dayOfMonth().withMaximumValue())){
//						List<PersonDay> pdList2 = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ? and pd.date <= ?", 
//								pd.person, pd.date, new LocalDate(pd.date).dayOfMonth().withMaximumValue()).fetch();
//						for(PersonDay personday : pdList2){
//							personday.populatePersonDay();
//							personday.save();
//						}
//					}
				}
				flash.success("Inserito il codice di assenza per il periodo richiesto");
				render("@save");

			}
			else{
				Logger.debug("%s %s è in turno, reperibilità", person.name, person.surname);
				flash.error("Non si può inserire un giorno di ferie per %s %s che è in turno/reperibilità. \n Contattarlo e chiedere spiegazioni", 
						person.name, person.surname);
				//FIXME: e dopo aver ricevuto le spiegazioni come forzo l'inserimento??
				render("@save");
			}

		}

		/**
		 * controllo che le persone che richiedono il riposo compensativo, che hanno una qualifica compresa tra 1 e 3, non abbiano superato
		 * il massimo numero di giorni di riposo compensativo consentiti e presenti in configurazione
		 */
		if(absenceType.code.equals("91") && person.qualification.qualification > 0 && person.qualification.qualification < 4){
			Logger.debug("Devo inserire un codice %s per %s %s", absenceType.code, person.name, person.surname);
			Configuration config = Configuration.getCurrentConfiguration();
			LocalDate actualDate = new LocalDate(yearFrom, monthFrom, dayFrom);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
				pd = new PersonDay(person, actualDate);
				pd.create();
			}
			//TODO: Fare un'unica select per estrarre il count delle absences con absenceType.code = 94 
			Query query = JPA.em().createQuery("SELECT abs FROM Absence abs WHERE abs.personDay.person = :person "+ 
					"AND abs.personDay.date between :dateStart AND :dateTo AND abs.absenceType.code = :code");
			query.setParameter("person", pd.person).
			setParameter("dateStart", new LocalDate(yearFrom, 1,1)).
			setParameter("dateTo",actualDate).
			setParameter("code", "91");
			List<Object> resultList = query.getResultList();
			Logger.debug("Il numero di assenze con codice %s fino a oggi è %d", absenceType.code, resultList.size());
			if(resultList.size() >= config.maxRecoveryDaysOneThree){
				flash.error("Il dipendente %s %s non può usufruire del codice di assenza %s poichè ha raggiunto il limite previsto per" +
						"quel codice", person.name, person.surname, absenceType.code);
				render("@save");
				return;
			}

		}

		//TODO: implementare i controlli sui gruppi di codici di assenza, i controlli sui gruppi devono anche implementare
		// le sostituzioni dei codici tramite accumulutatori o query ad hoc
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, dateFrom).first();
		if(pd == null) {
			pd = new PersonDay(person, dateFrom);
			pd.create();
		}
		Logger.debug("Creato il personDay %s", pd);
		Absence absence = new Absence();
		Logger.debug("%s %s può usufruire del codice %s", person.name, person.surname, absenceType.code);
		if(dateTo.isBefore(dateFrom) || dateTo.isEqual(dateFrom)){
			Logger.debug("Si intende inserire un'assenza per un giorno solo");
			
			
			if(params.get("datasize", Blob.class) != null){
				absence.absenceRequest = params.get("datasize", Blob.class);
			}
			else 
				absence.absenceRequest = null;

			absence.absenceType = absenceType;
			
			pd.addAbsence(absence);
			pd.save();
			Logger.debug("Creata e salvata l'assenza %s con codice %s", absence, absence.absenceType.code);
			pd.populatePersonDay();
			
			if(pd.date.isBefore(new LocalDate(pd.date).dayOfMonth().withMaximumValue())){
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ? and pd.date <= ?", 
						pd.person, pd.date, new LocalDate(pd.date).dayOfMonth().withMaximumValue()).fetch();
				for(PersonDay personday : pdList){
					personday.populatePersonDay();
					personday.save();
				}
			}

			flash.success(
					String.format("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name));
			render("@save");

		}
		else{

			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
					person, dateFrom, dateTo).fetch();
			if(pdList.size() != 0){

				for(PersonDay pdInside : pdList){
					absence = new Absence();
					absence.absenceType = absenceType;
					absence.personDay = pdInside;
					absence.save();
					pdInside.populatePersonDay();
					pdInside.save();
				}
			}
			else{

				while(!dateFrom.isAfter(dateTo)){
					Logger.debug("Datefrom: %s DateTo: %s", dateFrom, dateTo);
					PersonDay pdInside = PersonUtility.createPersonDayFromDate(person, dateFrom);
					if(pdInside != null){
						pdInside.create();
						Logger.debug("Creato personDay per il giorno %s ", dateFrom);

						absence = new Absence();
						absence.absenceType = absenceType;
						absence.personDay = pdInside;
						absence.save();
						pdInside.populatePersonDay();
						pdInside.save();
					}
					dateFrom = dateFrom.plusDays(1);
				}
			}
			List<PersonDay> otherPdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ?", 
					pd.person, pd.date).fetch();
			for(PersonDay p : otherPdList){
				if(p.date.getMonthOfYear() == absence.personDay.date.getMonthOfYear()){
					p.populatePersonDay();
					p.save();
				}
				
			}
			flash.success("Inserita assenza %s dal %s al %s", absenceType.code, dateFrom, dateTo);
			render("@save");
		}

	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void editCode(@Required Long absenceCodeId) throws InstantiationException, IllegalAccessException{
		AbsenceType abt = AbsenceType.findById(absenceCodeId);
		List<JustifiedTimeAtWork> justList = new ArrayList<JustifiedTimeAtWork>();
		justList.add(0,JustifiedTimeAtWork.AllDay);
		justList.add(1,JustifiedTimeAtWork.HalfDay);
		justList.add(2,JustifiedTimeAtWork.OneHour);
		justList.add(3,JustifiedTimeAtWork.TwoHours);
		justList.add(4,JustifiedTimeAtWork.ThreeHours);
		justList.add(5,JustifiedTimeAtWork.FourHours);
		justList.add(6,JustifiedTimeAtWork.FiveHours);
		justList.add(7,JustifiedTimeAtWork.SixHours);
		justList.add(8,JustifiedTimeAtWork.SevenHours);
		justList.add(9,JustifiedTimeAtWork.EightHours);
		justList.add(10,JustifiedTimeAtWork.Nothing);
		justList.add(11,JustifiedTimeAtWork.TimeToComplete);
		justList.add(12,JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours);
		
		List<Qualification> qualList = Qualification.findAll();
		List<AccumulationType> accType = new ArrayList<AccumulationType>();
		accType.add(0, AccumulationType.always);
		accType.add(1, AccumulationType.monthly);
		accType.add(2, AccumulationType.no);
		accType.add(3, AccumulationType.yearly);
		List<AccumulationBehaviour> behaviourType = new ArrayList<AccumulationBehaviour>();
		behaviourType.add(0, AccumulationBehaviour.nothing);
		behaviourType.add(1, AccumulationBehaviour.noMoreAbsencesAccepted);
		behaviourType.add(2, AccumulationBehaviour.replaceCodeAndDecreaseAccumulation);
		
		render(abt, justList, qualList, accType, behaviourType);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void updateCode(){
		AbsenceType absence = AbsenceType.findById(params.get("absenceTypeId", Long.class));
		if(absence == null)
			notFound();
		Logger.debug("Il codice d'assenza da modificare è %s", absence.code);
		absence.description = params.get("descrizione");
		Logger.debug("Il valore di uso interno è: %s", params.get("usoInterno", Boolean.class));
		Logger.debug("Il valore di uso multiplo è: %s", params.get("usoMultiplo", Boolean.class));
		Logger.debug("Il valore di tempo giustificato è: %s", params.get("abt.justifiedTimeAtWork"));
		absence.internalUse = params.get("usoInterno", Boolean.class);		
		absence.multipleUse = params.get("usoMultiplo", Boolean.class);
		absence.validFrom = new LocalDate(params.get("inizio"));
		absence.validTo = new LocalDate(params.get("fine"));
		String justifiedTimeAtWork = params.get("abt.justifiedTimeAtWork");			
		absence.justifiedTimeAtWork = JustifiedTimeAtWork.getByDescription(justifiedTimeAtWork);
		
		for(int i = 1; i <= 10; i++){
			if(params.get("qualification"+i) != null){
				Qualification q = Qualification.findById(new Long(i));
				if(!absence.qualifications.contains(q))
					absence.qualifications.add(q);
			}
			else{
				Qualification q = Qualification.findById(new Long(i));
				if(absence.qualifications.contains(q))
					absence.qualifications.remove(q);
			}
		}
		
			
		absence.mealTicketCalculation = params.get("calcolaBuonoPasto", Boolean.class);
		absence.ignoreStamping = params.get("ignoraTimbrature", Boolean.class);
		if(!params.get("gruppo").equals("")){
			absence.absenceTypeGroup.label = params.get("gruppo");
			absence.absenceTypeGroup.accumulationBehaviour = AccumulationBehaviour.getByDescription((params.get("abt.absenceTypeGroup.accumulationBehaviour")));
			absence.absenceTypeGroup.accumulationType = AccumulationType.getByDescription((params.get("abt.absenceTypeGroup.accumulationType")));
			absence.absenceTypeGroup.limitInMinute = params.get("limiteAccumulo", Integer.class);
			absence.absenceTypeGroup.minutesExcess = params.get("minutiEccesso", Boolean.class);
			String codeToReplace = params.get("codiceSostituzione");
			AbsenceTypeGroup abtg = AbsenceTypeGroup.find("Select abtg from AbsenceTypeGroup abtg where abtg.code = ?", codeToReplace).first();
			absence.absenceTypeGroup = abtg;
		}
		absence.save();
		
		flash.success("Modificato codice di assenza %s", absence.code);
		render("@Stampings.redirectToIndex");
		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void edit(@Required Long absenceId) {
		Logger.debug("Edit absence called for absenceId=%d", absenceId);

		Absence absence = Absence.findById(absenceId);
		if (absence == null) {
			notFound();
		}
		List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();

		List<AbsenceType> allCodes = getAllAbsenceTypes(absence.personDay.date);
		render(absence, frequentAbsenceTypeList, allCodes);				
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void update() {
		Absence absence = Absence.findById(params.get("absenceId", Long.class));
		if (absence == null) {
			notFound();
		}
		String oldAbsenceCode = absence.absenceType.code;
		String absenceCode = params.get("absenceCode");
		if (absenceCode == null || absenceCode.isEmpty()) {
			PersonDay pd = absence.personDay;
			absence.delete();
			pd.absences.remove(absence);
			pd.populatePersonDay();
			pd.save();
			if(pd.date.isBefore(new LocalDate(pd.date).dayOfMonth().withMaximumValue())){
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ? and pd.date <= ?", 
						pd.person, pd.date, new LocalDate(pd.date).dayOfMonth().withMaximumValue()).fetch();
				for(PersonDay personday : pdList){
					personday.populatePersonDay();
					personday.save();
				}
			}
			flash.success("Timbratura di tipo %s per il giorno %s rimossa per il dipendente %s %s", 
					oldAbsenceCode, PersonTags.toDateTime(absence.personDay.date), pd.person.name, pd.person.surname);			
		} 
		else {

			AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
			Logger.debug("AbsenceType: %s", absenceType.code);
			PersonDay pd = absence.personDay;
			Logger.debug("PersonDay: %s", pd);
			Absence existingAbsence = Absence.find("Select a from Absence a where a.personDay = ? and a.id <> ?", pd, absence.id).first();

			if(existingAbsence != null){
				validation.keep();
				params.flash();
				flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(absence.personDay.date));
				edit(absence.id);
				render("@edit");
			}

			if((params.get("buonoMensaSi") != null) && params.get("buonoMensaSi", Boolean.class) == true){

				pd.isTicketAvailable = true;
				pd.save();

			}
			if((params.get("buonoMensaNo") != null) && params.get("buonoMensaNo", Boolean.class) == true){
				pd.isTicketAvailable = false;
				pd.save();
			}

			if((params.get("buonoMensaCalcolato") != null) && params.get("buonoMensaCalcolato", Boolean.class) == true){
				pd.populatePersonDay();
				pd.save();
			}



			if(params.get("datasize", Blob.class) != null){
				absence.absenceRequest = params.get("datasize", Blob.class);
			}
			absence.absenceType = absenceType;
			absence.save();

			flash.success(
					String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", 
							PersonTags.toDateTime(absence.personDay.date), absence.personDay.person.surname, absence.personDay.person.name, absenceCode));
		}
		render("@save");
	}



}
