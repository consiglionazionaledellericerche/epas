package manager.recaps;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.MealTicketDao;
import dao.PersonDayDao;

public class PersonResidualMonthRecap {

	public Person person;
	public Contract contract;
	
	public String contractDescription;
	
	public int qualifica;
	public boolean possibileUtilizzareResiduoAnnoPrecedente = true;
	public PersonResidualMonthRecap mesePrecedente;
	public int anno;
	public int mese;
	
	public int initMonteOreAnnoPassato;
	public int initMonteOreAnnoCorrente;
	
	public int initResiduoAnnoCorrenteNelMese = 0;	//per il template (se sourceContract è del mese)
	
	public int progressivoFinaleMese		 = 0;	//person day
	public int progressivoFinalePositivoMese = 0;	//person day
	public int progressivoFinaleNegativoMese = 0;	//person day
	
	public int progressivoFinalePositivoMesePrint = 0;	//per il template
	
	public int straordinariMinuti 			 = 0;	//competences
	public int straordinariMinutiS1Print	 = 0;	//per il template
	public int straordinariMinutiS2Print	 = 0;	//per il template
	public int straordinariMinutiS3Print	 = 0;	//per il template
	
	public int riposiCompensativiMinuti 	 = 0;	//absences 
	public int riposiCompensativiMinutiPrint = 0;	//per il template
	
	
	public int progressivoFinaleNegativoMeseImputatoAnnoPassato;
	public int progressivoFinaleNegativoMeseImputatoAnnoCorrente;
	public int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
	
	public int riposiCompensativiMinutiImputatoAnnoPassato;
	public int riposiCompensativiMinutiImputatoAnnoCorrente;
	public int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
	
	public int monteOreAnnoPassato;
	public int monteOreAnnoCorrente;
	public int numeroRiposiCompensativi;
	
	public int oreLavorate = 0;
	
	public int buoniPastoDalMesePrecedente = 0;
	public int buoniPastoConsegnatiNelMese = 0;
	public int buoniPastoUsatiNelMese = 0;
	public int buoniPastoResidui = 0;
	
	private PersonResidualMonthRecap() {}
	
	/**
	 * Costruisce un oggetto mese con tutte le informazioni necessarie al calcolo della situazione residuo annuale della persona nell'ambito del contratto passato come argomento.
	 * Visibile solo all'interno del package models.personalMonthSituation.
	 * @param mesePrecedente
	 * @param year
	 * @param month
	 * @param contract
	 * @param initMonteOreAnnoPassato
	 * @param initMonteOreAnnoCorrente
	 * @param validDataForPersonDay
	 * @param validDataForCompensatoryRest 
	 */
	public static PersonResidualMonthRecap factory(PersonResidualMonthRecap mesePrecedente, int year, int month, Contract contract, 
			int initMonteOreAnnoPassato, int initMonteOreAnnoCorrente, int initMealTickets,
			DateInterval validDataForPersonDay, DateInterval validDataForCompensatoryRest, DateInterval validDataForMealTickets) {
		
		
		PersonResidualMonthRecap newMese = new PersonResidualMonthRecap();
		
		newMese.contract = contract;
		newMese.person = contract.person;
		newMese.qualifica = newMese.person.qualification.qualification;
		newMese.anno = year;
		newMese.mese = month;

		newMese.initMonteOreAnnoCorrente = initMonteOreAnnoCorrente;
		newMese.initMonteOreAnnoPassato = initMonteOreAnnoPassato;
		
		
		//Per stampare a video il residuo da inizializzazione se riferito al mese
		if(contract.sourceDate != null && 
				contract.sourceDate.getMonthOfYear() == month && contract.sourceDate.getYear() == year) {
			newMese.initResiduoAnnoCorrenteNelMese = contract.sourceRemainingMinutesCurrentYear;
		}
		
		setContractDescription(newMese);
		
		//Inizializzazione residui
		//Gennaio
		if(month==1)
		{
			newMese.mesePrecedente = null;
			newMese.monteOreAnnoPassato = initMonteOreAnnoPassato;
			newMese.monteOreAnnoCorrente = initMonteOreAnnoCorrente;
			
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(newMese.monteOreAnnoPassato<0)
			{
				newMese.progressivoFinalePositivoMese = newMese.progressivoFinalePositivoMese + newMese.monteOreAnnoPassato;
				newMese.monteOreAnnoPassato = 0;
			}
		}
		
		//Febbraio / Marzo
		else if(month==2 || month==3)
		{
			newMese.mesePrecedente = mesePrecedente;
			newMese.monteOreAnnoPassato = initMonteOreAnnoPassato;
			newMese.monteOreAnnoCorrente= initMonteOreAnnoCorrente;
		}
		
		// Aprile -> Dicembre
		else
		{
			newMese.mesePrecedente = mesePrecedente;
			newMese.monteOreAnnoPassato = initMonteOreAnnoPassato;
			newMese.monteOreAnnoCorrente= initMonteOreAnnoCorrente;
			
			if(newMese.qualifica>3)
			{
				newMese.possibileUtilizzareResiduoAnnoPrecedente = false;
				newMese.monteOreAnnoPassato = 0;
			}
		}
		
		//Inizializzazione buoni pasto
		if(month==1) 
		{
			newMese.mesePrecedente = null;
			newMese.buoniPastoDalMesePrecedente = initMealTickets;
		}
		else if(newMese.mesePrecedente != null)
		{
			newMese.buoniPastoDalMesePrecedente = 
					mesePrecedente.buoniPastoDalMesePrecedente 
					+ mesePrecedente.buoniPastoConsegnatiNelMese
					- mesePrecedente.buoniPastoUsatiNelMese;
		}
		
		setMealTicketsInformation(newMese, validDataForMealTickets);
		
		setPersonDayInformation(newMese, validDataForPersonDay);
		setPersonMonthInformation(newMese, validDataForCompensatoryRest);
		

		assegnaProgressivoFinaleNegativo(newMese);
		assegnaStraordinari(newMese);
		assegnaRiposiCompensativi(newMese);
		
		//All'anno corrente imputo sia ciò che ho imputato al residuo del mese precedente dell'anno corrente sia ciò che ho imputato al progressivo finale positivo del mese
		//perchè non ho interesse a visualizzarli separati nel template. 
		newMese.progressivoFinaleNegativoMeseImputatoAnnoCorrente = newMese.progressivoFinaleNegativoMeseImputatoAnnoCorrente + newMese.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
		newMese.riposiCompensativiMinutiImputatoAnnoCorrente = newMese.riposiCompensativiMinutiImputatoAnnoCorrente + newMese.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
		
		//Al monte ore dell'anno corrente aggiungo ciò che non ho utilizzato del progressivo finale positivo del mese
		newMese.monteOreAnnoCorrente = newMese.monteOreAnnoCorrente + newMese.progressivoFinalePositivoMese;
		
		return newMese;
	}
	
	/**
	 * Ritorna il numero di ore disponibili per straordinari per la persona nel mese.
	 * Calcola il residuo positivo del mese per straordinari inerente il contratto attivo nel mese.
	 * Nel caso di due contratti attivi nel mese viene ritornato il valore per il contratto più recente.
	 * Nel caso di nessun contratto attivo nel mese viene ritornato il valore 0.
	 * @param person
	 * @param year
	 * @param month
	 */
	public static Integer positiveResidualInMonth(Person person, int year, int month){
		List<Contract> monthContracts = person.getMonthContracts(month, year);
		for(Contract contract : monthContracts)
		{
			if(contract.isLastInMonth(month, year))
			{
				PersonResidualYearRecap c = 
						PersonResidualYearRecap.factory(contract, year, null);
				if(c.getMese(month)!=null)
					return c.getMese(month).progressivoFinalePositivoMese;
			}
		}
		return 0;
	}
	

	
	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private static void setPersonDayInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForPersonDay)
	{
		if(validDataForPersonDay!=null)
		{
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriodDesc(monthRecap.person, validDataForPersonDay.getBegin(), validDataForPersonDay.getEnd(), true);
//			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date desc",
//					monthRecap.person, validDataForPersonDay.getBegin(), validDataForPersonDay.getEnd()).fetch();

			//progressivo finale fine mese
			for(PersonDay pd : pdList){
				if(pd != null){
					monthRecap.progressivoFinaleMese = pd.progressive;
					break;
				}
				else{
					//
				}
			}

			//progressivo finale positivo e negativo mese
			for(PersonDay pd : pdList)
			{
				if(pd.difference>=0)
					monthRecap.progressivoFinalePositivoMese += pd.difference;
				else
					monthRecap.progressivoFinaleNegativoMese += pd.difference;
				
				monthRecap.oreLavorate += pd.timeAtWork;
			}
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese*-1;

			monthRecap.progressivoFinalePositivoMesePrint = monthRecap.progressivoFinalePositivoMese;
			
		}
	}
	
	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private static void setMealTicketsInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForMealTickets)
	{
		
		if(validDataForMealTickets!=null)
		{
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(monthRecap.person, validDataForMealTickets.getBegin(), validDataForMealTickets.getEnd(), true);
//			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date",
//					monthRecap.person, validDataForMealTickets.getBegin(), validDataForMealTickets.getEnd()).fetch();

			//buoni pasto utilizzati
			for(PersonDay pd : pdList){
				if(pd != null && pd.isTicketAvailable){
					monthRecap.buoniPastoUsatiNelMese++;
				}
			}
			
			//Numero ticket consegnati nel mese
			monthRecap.buoniPastoConsegnatiNelMese = 
					MealTicketDao.getMealTicketAssignedToPersonIntoInterval(
							monthRecap.contract, validDataForMealTickets).size();
			
			//residuo
			monthRecap.buoniPastoResidui = monthRecap.buoniPastoDalMesePrecedente + monthRecap.buoniPastoConsegnatiNelMese - monthRecap.buoniPastoUsatiNelMese;
						
		}
	}
	
	/**
	 * 
	 * @param validDataForCompensatoryRest, l'intervallo all'interno del quale ricercare i riposi compensativi
	 */
	private static void setPersonMonthInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForCompensatoryRest)
	{
		CompetenceCode s1 = CompetenceCodeDao.getCompetenceCodeByCode("S1");
		CompetenceCode s2 = CompetenceCodeDao.getCompetenceCodeByCode("S2");
		CompetenceCode s3 = CompetenceCodeDao.getCompetenceCodeByCode("S3");
		if(monthRecap.contract.isLastInMonth(monthRecap.mese, monthRecap.anno))	//gli straordinari li assegno solo all'ultimo contratto attivo del mese
		{
			//straordinari s1
			Competence competenceS1 = CompetenceDao.getCompetence(monthRecap.person, monthRecap.anno, monthRecap.mese, s1);
//			List<Competence> competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
//					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", monthRecap.person, monthRecap.anno, monthRecap.mese, "S1").fetch();
//			for(Competence comp : competenceList)
//			{
			if(competenceS1 != null)
				monthRecap.straordinariMinutiS1Print = monthRecap.straordinariMinutiS1Print + (competenceS1.valueApproved * 60);
			else
				monthRecap.straordinariMinutiS1Print = 0;
//			}

			//straordinari s2
			Competence competenceS2 = CompetenceDao.getCompetence(monthRecap.person, monthRecap.anno, monthRecap.mese, s2);
			
//			competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
//					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", monthRecap.person, monthRecap.anno, monthRecap.mese, "S2").fetch();
//			for(Competence comp : competenceList)
//			{
			if(competenceS2 != null)
				monthRecap.straordinariMinutiS2Print = monthRecap.straordinariMinutiS2Print + (competenceS2.valueApproved * 60);
			else
				monthRecap.straordinariMinutiS2Print = 0;
//			}

			//straordinari s3
			Competence competenceS3 = CompetenceDao.getCompetence(monthRecap.person, monthRecap.anno, monthRecap.mese, s3);
//			competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
//					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", monthRecap.person, monthRecap.anno, monthRecap.mese, "S3").fetch();
//			for(Competence comp : competenceList)
//			{
			if(competenceS3 != null)
				monthRecap.straordinariMinutiS3Print = monthRecap.straordinariMinutiS3Print + (competenceS3.valueApproved * 60);
			else
				monthRecap.straordinariMinutiS3Print = 0;
//			}

			monthRecap.straordinariMinuti = monthRecap.straordinariMinutiS1Print + monthRecap.straordinariMinutiS2Print + monthRecap.straordinariMinutiS3Print;
		}
		
		if(validDataForCompensatoryRest!=null)
		{
			List<Absence> riposiCompensativi = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(monthRecap.person), Optional.fromNullable("S1"), 
					validDataForCompensatoryRest.getBegin(), validDataForCompensatoryRest.getEnd(), 
					Optional.<JustifiedTimeAtWork>absent(), false, false);
//			List<Absence> riposiCompensativi = Absence.find("Select abs from Absence abs, AbsenceType abt, PersonDay pd where abs.personDay = pd and abs.absenceType = abt and abt.code = ? and pd.person = ? "
//					+ "and pd.date between ? and ?", "91", monthRecap.person, validDataForCompensatoryRest.getBegin(), validDataForCompensatoryRest.getEnd()).fetch();
			monthRecap.riposiCompensativiMinuti = 0;
			monthRecap.numeroRiposiCompensativi = 0;
			for(Absence abs : riposiCompensativi){
				monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti + monthRecap.person.getWorkingTimeType(abs.personDay.date).getWorkingTimeTypeDayFromDayOfWeek(abs.personDay.date.getDayOfWeek()).workingTime;
				monthRecap.numeroRiposiCompensativi++;
			}
			monthRecap.riposiCompensativiMinutiPrint = monthRecap.riposiCompensativiMinuti;
			
		}		

	}
	
	private static void assegnaProgressivoFinaleNegativo(PersonResidualMonthRecap monthRecap)
	{
		
		//quello che assegno al monte ore passato
		if(monthRecap.progressivoFinaleNegativoMese < monthRecap.monteOreAnnoPassato)
		{
			monthRecap.monteOreAnnoPassato = monthRecap.monteOreAnnoPassato - monthRecap.progressivoFinaleNegativoMese;
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato = monthRecap.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato = monthRecap.monteOreAnnoPassato;
			monthRecap.monteOreAnnoPassato = 0;
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese - monthRecap.progressivoFinaleNegativoMeseImputatoAnnoPassato;
		}
		
		//quello che assegno al monte ore corrente
		if(monthRecap.progressivoFinaleNegativoMese < monthRecap.monteOreAnnoCorrente)
		{
			monthRecap.monteOreAnnoCorrente = monthRecap.monteOreAnnoCorrente - monthRecap.progressivoFinaleNegativoMese;
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente = monthRecap.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente = monthRecap.monteOreAnnoCorrente;
			monthRecap.monteOreAnnoCorrente = 0;
			monthRecap.progressivoFinaleNegativoMese = monthRecap.progressivoFinaleNegativoMese - monthRecap.progressivoFinaleNegativoMeseImputatoAnnoCorrente;
		}
		
		//quello che assegno al progressivo positivo del mese
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.progressivoFinaleNegativoMese;
		monthRecap.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = monthRecap.progressivoFinaleNegativoMese;
		return;
		
	}
	
	private static void assegnaStraordinari(PersonResidualMonthRecap monthRecap)
	{
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.straordinariMinuti;
	}
	
	private static void assegnaRiposiCompensativi(PersonResidualMonthRecap monthRecap)
	{
		//quello che assegno al monte ore passato
		if(monthRecap.riposiCompensativiMinuti < monthRecap.monteOreAnnoPassato)
		{
			monthRecap.monteOreAnnoPassato = monthRecap.monteOreAnnoPassato - monthRecap.riposiCompensativiMinuti;
			monthRecap.riposiCompensativiMinutiImputatoAnnoPassato = monthRecap.riposiCompensativiMinuti;
			return;
		}
		else
		{
			monthRecap.riposiCompensativiMinutiImputatoAnnoPassato = monthRecap.monteOreAnnoPassato;
			monthRecap.monteOreAnnoPassato = 0;
			monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti - monthRecap.riposiCompensativiMinutiImputatoAnnoPassato;
		}
		
		//quello che assegno al monte ore corrente
		if(monthRecap.riposiCompensativiMinuti < monthRecap.monteOreAnnoCorrente)
		{
			monthRecap.monteOreAnnoCorrente = monthRecap.monteOreAnnoCorrente - monthRecap.riposiCompensativiMinuti;
			monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente = monthRecap.riposiCompensativiMinuti;
			return;
		}
		else
		{
			monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente = monthRecap.monteOreAnnoCorrente;
			monthRecap.monteOreAnnoCorrente = 0;
			monthRecap.riposiCompensativiMinuti = monthRecap.riposiCompensativiMinuti - monthRecap.riposiCompensativiMinutiImputatoAnnoCorrente;
		}
		//quello che assegno al progressivo positivo del mese
		monthRecap.progressivoFinalePositivoMese = monthRecap.progressivoFinalePositivoMese - monthRecap.riposiCompensativiMinuti;
		monthRecap.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = monthRecap.riposiCompensativiMinuti;
	
	}	
	
	/**
	 * Costruisce una stringa di descrizione per il contratto utilizzata in stampings.html e personStampings.html
	 */
	private static void setContractDescription(PersonResidualMonthRecap monthRecap)
	{
		LocalDate beginMonth = new LocalDate(monthRecap.anno, monthRecap.mese, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(beginMonth, endMonth);	
		LocalDate endContract = monthRecap.contract.expireContract;
		if(monthRecap.contract.endContract!=null)
			endContract = monthRecap.contract.endContract;
		
		if(DateUtility.isDateIntoInterval(endContract, monthInterval))
			monthRecap.contractDescription = "(contratto scaduto in data " + endContract+")";
		else
			monthRecap.contractDescription = "";
		
	}
	
	
}


