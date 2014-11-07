package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Absence;
import models.Competence;
import models.Contract;
import models.ContractYearRecap;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;

/**
 * 
 * @author alessandro
 *
 */
public class PersonResidualManager {

	/**
	 * Costruisce la situazione annuale residuale della persona.
	 * @param contract
	 * @param year
	 * @param calcolaFinoA valorizzare questo campo per fotografare la situazione residuale in un certo momento 
	 *   (ad esempio se si vuole verificare la possibilità di prendere riposo compensativo in un determinato giorno). 
	 *   Null se si desidera la situazione residuale a oggi. 
	 */
	public static PersonResidualYearRecap build(Contract contract, int year, LocalDate calcolaFinoA) {

		PersonResidualYearRecap csap = new PersonResidualYearRecap();

		int firstMonthToCompute = 1;
		LocalDate firstDayInDatabase = new LocalDate(year,1,1);
		int initMonteOreAnnoPassato = 0;
		int initMonteOreAnnoCorrente = 0;

		if(contract==null)
		{
			return null;
		}	

		//Recupero situazione iniziale dell'anno richiesto
		ContractYearRecap recapPreviousYear = contract.getContractYearRecap(year-1);
		if(recapPreviousYear!=null)	
		{
			initMonteOreAnnoPassato = recapPreviousYear.remainingMinutesCurrentYear + recapPreviousYear.remainingMinutesLastYear;
		}
		if(contract.sourceDate!=null && contract.sourceDate.getYear()==year)
		{
			initMonteOreAnnoPassato = contract.sourceRemainingMinutesLastYear;
			initMonteOreAnnoCorrente = contract.sourceRemainingMinutesCurrentYear;
			firstDayInDatabase = contract.sourceDate.plusDays(1);
			firstMonthToCompute = contract.sourceDate.getMonthOfYear();
		}

		csap.mesi = new ArrayList<PersonResidualMonthRecap>();
		PersonResidualMonthRecap previous = null;
		int actualMonth = firstMonthToCompute;
		int endMonth = 12;
		if(new LocalDate().getYear()==year)
			endMonth = Math.min(endMonth, new LocalDate().getMonthOfYear());
		while(actualMonth<=endMonth)
		{
			//Prendo la situazione iniziale del mese (se previous è null sono i valori calcolati precedentemente)
			if(previous!=null)
			{
				initMonteOreAnnoPassato = previous.monteOreAnnoPassato;
				initMonteOreAnnoCorrente = previous.monteOreAnnoCorrente;
			}

			LocalDate today = LocalDate.now();

			//////////////////////////////////////////////////////////////////////////////////////////////////////////
			//	Intervallo per progressivi
			//////////////////////////////////////////////////////////////////////////////////////////////////////////

			// 1) Tutti i giorni del mese

			LocalDate monthBeginForPersonDay = new LocalDate(year, actualMonth, 1);
			LocalDate monthEndForPersonDay = monthBeginForPersonDay.dayOfMonth().withMaximumValue();
			DateInterval monthIntervalForPersonDay = new DateInterval(monthBeginForPersonDay, monthEndForPersonDay);

			// 2) Nel caso del calcolo del mese attuale

			if( DateUtility.isDateIntoInterval(today, monthIntervalForPersonDay) )
			{
				// 2.1) Se oggi non è il primo giorno del mese allora tutti i giorni del mese fino a ieri.

				if ( today.getDayOfMonth() != 1 )
				{
					monthEndForPersonDay = today.minusDays(1);
					monthIntervalForPersonDay = new DateInterval(monthBeginForPersonDay, monthEndForPersonDay);
				}

				// 2.2) Se oggi è il primo giorno del mese allora null.

				else
				{
					monthIntervalForPersonDay = null;
				}
			}

			// 3) Filtro per dati nel database e estremi del contratto

			DateInterval validDataForPersonDay = null;
			if(monthIntervalForPersonDay != null)
			{
				DateInterval requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
				DateInterval contractInterval = contract.getContractDateInterval();
				validDataForPersonDay = DateUtility.intervalIntersection(monthIntervalForPersonDay, requestInterval);
				validDataForPersonDay = DateUtility.intervalIntersection(validDataForPersonDay, contractInterval);
			}


			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//	Intervallo per riposi compensativi
			////////////////////////////////////////////////////////////////////////////////////////////////////////////

			// 1) Tutti i giorni del mese

			LocalDate monthBeginForCompensatoryRest = new LocalDate(year, actualMonth, 1);
			LocalDate monthEndForCompensatoryRest = monthBeginForCompensatoryRest.dayOfMonth().withMaximumValue();
			DateInterval monthIntervalForCompensatoryRest = new DateInterval(monthBeginForCompensatoryRest, monthEndForCompensatoryRest);

			// 2) Nel caso del mese attuale considero anche il mese successivo

			if( DateUtility.isDateIntoInterval(today, monthIntervalForCompensatoryRest) ) 
			{
				monthEndForCompensatoryRest = monthEndForCompensatoryRest.plusMonths(1).dayOfMonth().withMaximumValue();
				monthIntervalForCompensatoryRest = new DateInterval(monthBeginForCompensatoryRest, monthEndForCompensatoryRest);
			}

			// 3) Filtro per dati nel database e estremi del contratto

			DateInterval validDataForCompensatoryRest = null;
			DateInterval contractInterval = contract.getContractDateInterval();
			validDataForCompensatoryRest = DateUtility.intervalIntersection(monthIntervalForCompensatoryRest, contractInterval);

			//Costruisco l'oggetto
			PersonResidualMonthRecap mese = 
					PersonResidualManager.buildPersonResidualMonthRecap(previous, year, actualMonth, contract, 
					initMonteOreAnnoPassato, initMonteOreAnnoCorrente, 
					validDataForPersonDay, validDataForCompensatoryRest);
			csap.mesi.add(mese);
			previous = mese;
			actualMonth++;	
		}

		return csap;
	}
	
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
	private static PersonResidualMonthRecap buildPersonResidualMonthRecap(PersonResidualMonthRecap mesePrecedente, int year, int month, Contract contract, 
			int initMonteOreAnnoPassato, int initMonteOreAnnoCorrente, 
			DateInterval validDataForPersonDay, DateInterval validDataForCompensatoryRest) {
		
		
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
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	private static void setPersonDayInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForPersonDay)
	{
		if(validDataForPersonDay!=null)
		{
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date desc",
					monthRecap.person, validDataForPersonDay.getBegin(), validDataForPersonDay.getEnd()).fetch();

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
	 * @param validDataForCompensatoryRest, l'intervallo all'interno del quale ricercare i riposi compensativi
	 */
	private static void setPersonMonthInformation(PersonResidualMonthRecap monthRecap, DateInterval validDataForCompensatoryRest)
	{
		
		if(monthRecap.contract.isLastInMonth(monthRecap.mese, monthRecap.anno))	//gli straordinari li assegno solo all'ultimo contratto attivo del mese
		{
			//straordinari s1
			List<Competence> competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", monthRecap.person, monthRecap.anno, monthRecap.mese, "S1").fetch();
			for(Competence comp : competenceList)
			{
				monthRecap.straordinariMinutiS1Print = monthRecap.straordinariMinutiS1Print + (comp.valueApproved * 60);
			}

			//straordinari s2
			competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", monthRecap.person, monthRecap.anno, monthRecap.mese, "S2").fetch();
			for(Competence comp : competenceList)
			{
				monthRecap.straordinariMinutiS2Print = monthRecap.straordinariMinutiS2Print + (comp.valueApproved * 60);
			}

			//straordinari s3
			competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", monthRecap.person, monthRecap.anno, monthRecap.mese, "S3").fetch();
			for(Competence comp : competenceList)
			{
				monthRecap.straordinariMinutiS3Print = monthRecap.straordinariMinutiS3Print + (comp.valueApproved * 60);
			}

			monthRecap.straordinariMinuti = monthRecap.straordinariMinutiS1Print + monthRecap.straordinariMinutiS2Print + monthRecap.straordinariMinutiS3Print;
		}
		
		if(validDataForCompensatoryRest!=null)
		{
			List<Absence> riposiCompensativi = Absence.find("Select abs from Absence abs, AbsenceType abt, PersonDay pd where abs.personDay = pd and abs.absenceType = abt and abt.code = ? and pd.person = ? "
					+ "and pd.date between ? and ?", "91", monthRecap.person, validDataForCompensatoryRest.getBegin(), validDataForCompensatoryRest.getEnd()).fetch();
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
						PersonResidualManager.build(contract, year, null);
				if(c.getMese(month)!=null)
					return c.getMese(month).progressivoFinalePositivoMese;
			}
		}
		return 0;
	}
	
}
