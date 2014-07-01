package models.personalMonthSituation;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.Absence;
import models.Competence;
import models.Contract;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;

public class Mese {

	public Person person;
	public Contract contract;
	
	public String contractDescription;
	
	public int qualifica;
	public boolean possibileUtilizzareResiduoAnnoPrecedente = true;
	public Mese mesePrecedente;
	public int anno;
	public int mese;
	
	public int initMonteOreAnnoPassato;
	public int initMonteOreAnnoCorrente;
	
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
	
	/**
	 * Costruisce un oggetto mese con tutte le informazioni necessarie al calcolo della situazione residuo annuale della persona nell'ambito del contratto passato come argomento.
	 * Visibile solo all'interno del package models.personalMonthSituation.
	 * @param mesePrecedente
	 * @param anno
	 * @param mese
	 * @param contract
	 * @param initMonteOreAnnoPassato
	 * @param initMonteOreAnnoCorrente
	 * @param validDataForPersonDay
	 * @param validDataForCompensatoryRest 
	 */
	protected Mese(Mese mesePrecedente, int anno, int mese, Contract contract, int initMonteOreAnnoPassato, int initMonteOreAnnoCorrente, DateInterval validDataForPersonDay, DateInterval validDataForCompensatoryRest)
	{
		this.contract = contract;
		this.person = contract.person;
		this.qualifica = person.qualification.qualification;
		this.anno = anno;
		this.mese = mese;

		this.initMonteOreAnnoCorrente = initMonteOreAnnoCorrente;
		this.initMonteOreAnnoPassato = initMonteOreAnnoPassato;
		
		setContractDescription();
		
		//Gennaio
		if(mese==1)
		{
			this.mesePrecedente = null;
			this.monteOreAnnoPassato = initMonteOreAnnoPassato;
			this.monteOreAnnoCorrente = initMonteOreAnnoCorrente;
			
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(this.monteOreAnnoPassato<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.monteOreAnnoPassato;
				this.monteOreAnnoPassato = 0;
			}
		}
		
		//Febbraio / Marzo
		else if(mese==2 || mese==3)
		{
			this.mesePrecedente = mesePrecedente;
			this.monteOreAnnoPassato = initMonteOreAnnoPassato;
			this.monteOreAnnoCorrente= initMonteOreAnnoCorrente;
		}
		
		// Aprile -> Dicembre
		else
		{
			this.mesePrecedente = mesePrecedente;
			this.monteOreAnnoPassato = initMonteOreAnnoPassato;
			this.monteOreAnnoCorrente= initMonteOreAnnoCorrente;
			
			if(qualifica>3)
			{
				this.possibileUtilizzareResiduoAnnoPrecedente = false;
				this.monteOreAnnoPassato = 0;
			}
		}
		
		setPersonDayInformation(validDataForPersonDay);
		setPersonMonthInformation(validDataForCompensatoryRest);
		

		assegnaProgressivoFinaleNegativo();
		assegnaStraordinari();
		assegnaRiposiCompensativi();
		
		//All'anno corrente imputo sia ciò che ho imputato al residuo del mese precedente dell'anno corrente sia ciò che ho imputato al progressivo finale positivo del mese
		//perchè non ho interesse a visualizzarli separati nel template. 
		this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = this.progressivoFinaleNegativoMeseImputatoAnnoCorrente + this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
		this.riposiCompensativiMinutiImputatoAnnoCorrente = this.riposiCompensativiMinutiImputatoAnnoCorrente + this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
		
		//Al monte ore dell'anno corrente aggiungo ciò che non ho utilizzato del progressivo finale positivo del mese
		this.monteOreAnnoCorrente = this.monteOreAnnoCorrente + this.progressivoFinalePositivoMese;
		
		return;
	}
	
	/**
	 * 
	 * @param validDataForPersonDay l'intervallo all'interno del quale ricercare i person day per il calcolo dei progressivi
	 */
	public void setPersonDayInformation(DateInterval validDataForPersonDay)
	{
		if(validDataForPersonDay!=null)
		{
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date desc",
					this.person, validDataForPersonDay.getBegin(), validDataForPersonDay.getEnd()).fetch();

			//progressivo finale fine mese
			for(PersonDay pd : pdList){
				if(pd != null){
					this.progressivoFinaleMese = pd.progressive;
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
					this.progressivoFinalePositivoMese += pd.difference;
				else
					this.progressivoFinaleNegativoMese += pd.difference;
				
				this.oreLavorate += pd.timeAtWork;
			}
			this.progressivoFinaleNegativoMese = this.progressivoFinaleNegativoMese*-1;

			this.progressivoFinalePositivoMesePrint = this.progressivoFinalePositivoMese;
		}
	}
	
	/**
	 * 
	 * @param validDataForCompensatoryRest, l'intervallo all'interno del quale ricercare i riposi compensativi
	 */
	public void setPersonMonthInformation(DateInterval validDataForCompensatoryRest)
	{
		
		if(this.contract.isLastInMonth(mese, anno))	//gli straordinari li assegno solo all'ultimo contratto attivo del mese
		{
			//straordinari s1
			List<Competence> competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", this.person, this.anno, this.mese, "S1").fetch();
			for(Competence comp : competenceList)
			{
				this.straordinariMinutiS1Print = this.straordinariMinutiS1Print + (comp.valueApproved * 60);
			}

			//straordinari s2
			competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", this.person, this.anno, this.mese, "S2").fetch();
			for(Competence comp : competenceList)
			{
				this.straordinariMinutiS2Print = this.straordinariMinutiS2Print + (comp.valueApproved * 60);
			}

			//straordinari s3
			competenceList = Competence.find("Select comp from Competence comp, CompetenceCode compCode where comp.competenceCode = compCode and comp.person = ?"
					+ "and comp.year = ? and comp.month = ? and compCode.code = ?", this.person, this.anno, this.mese, "S3").fetch();
			for(Competence comp : competenceList)
			{
				this.straordinariMinutiS3Print = this.straordinariMinutiS3Print + (comp.valueApproved * 60);
			}

			this.straordinariMinuti = this.straordinariMinutiS1Print + this.straordinariMinutiS2Print + this.straordinariMinutiS3Print;
		}
		
		if(validDataForCompensatoryRest!=null)
		{
			List<Absence> riposiCompensativi = Absence.find("Select abs from Absence abs, AbsenceType abt, PersonDay pd where abs.personDay = pd and abs.absenceType = abt and abt.code = ? and pd.person = ? "
					+ "and pd.date between ? and ?", "91", this.person, validDataForCompensatoryRest.getBegin(), validDataForCompensatoryRest.getEnd()).fetch();
			this.riposiCompensativiMinuti = 0;
			this.numeroRiposiCompensativi = 0;
			for(Absence abs : riposiCompensativi){
				this.riposiCompensativiMinuti = this.riposiCompensativiMinuti + this.person.getWorkingTimeType(abs.personDay.date).getWorkingTimeTypeDayFromDayOfWeek(abs.personDay.date.getDayOfWeek()).workingTime;
				this.numeroRiposiCompensativi++;
			}
			this.riposiCompensativiMinutiPrint = this.riposiCompensativiMinuti;
			
		}		

	}
	
	public void assegnaProgressivoFinaleNegativo()
	{
		
		//quello che assegno al monte ore passato
		if(this.progressivoFinaleNegativoMese < this.monteOreAnnoPassato)
		{
			this.monteOreAnnoPassato = this.monteOreAnnoPassato - this.progressivoFinaleNegativoMese;
			this.progressivoFinaleNegativoMeseImputatoAnnoPassato = this.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			this.progressivoFinaleNegativoMeseImputatoAnnoPassato = this.monteOreAnnoPassato;
			this.monteOreAnnoPassato = 0;
			this.progressivoFinaleNegativoMese = this.progressivoFinaleNegativoMese - this.progressivoFinaleNegativoMeseImputatoAnnoPassato;
		}
		
		//quello che assegno al monte ore corrente
		if(this.progressivoFinaleNegativoMese < this.monteOreAnnoCorrente)
		{
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente - this.progressivoFinaleNegativoMese;
			this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = this.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = this.monteOreAnnoCorrente;
			this.monteOreAnnoCorrente = 0;
			this.progressivoFinaleNegativoMese = this.progressivoFinaleNegativoMese - this.progressivoFinaleNegativoMeseImputatoAnnoCorrente;
		}
		
		//quello che assegno al progressivo positivo del mese
		this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.progressivoFinaleNegativoMese;
		this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = this.progressivoFinaleNegativoMese;
		return;
		
	}
	
	public void assegnaStraordinari()
	{
		this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.straordinariMinuti;
	}
	
	public void assegnaRiposiCompensativi()
	{
		//quello che assegno al monte ore passato
		if(this.riposiCompensativiMinuti < this.monteOreAnnoPassato)
		{
			this.monteOreAnnoPassato = this.monteOreAnnoPassato - this.riposiCompensativiMinuti;
			this.riposiCompensativiMinutiImputatoAnnoPassato = this.riposiCompensativiMinuti;
			return;
		}
		else
		{
			this.riposiCompensativiMinutiImputatoAnnoPassato = this.monteOreAnnoPassato;
			this.monteOreAnnoPassato = 0;
			this.riposiCompensativiMinuti = this.riposiCompensativiMinuti - this.riposiCompensativiMinutiImputatoAnnoPassato;
		}
		
		//quello che assegno al monte ore corrente
		if(this.riposiCompensativiMinuti < this.monteOreAnnoCorrente)
		{
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente - this.riposiCompensativiMinuti;
			this.riposiCompensativiMinutiImputatoAnnoCorrente = this.riposiCompensativiMinuti;
			return;
		}
		else
		{
			this.riposiCompensativiMinutiImputatoAnnoCorrente = this.monteOreAnnoCorrente;
			this.monteOreAnnoCorrente = 0;
			this.riposiCompensativiMinuti = this.riposiCompensativiMinuti - this.riposiCompensativiMinutiImputatoAnnoCorrente;
		}
		//quello che assegno al progressivo positivo del mese
		this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.riposiCompensativiMinuti;
		this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = this.riposiCompensativiMinuti;
	
	}	
	
	/**
	 * Costruisce una stringa di descrizione per il contratto utilizzata in stampings.html e personStampings.html
	 */
	public void setContractDescription()
	{
		LocalDate beginMonth = new LocalDate(this.anno, this.mese, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		DateInterval monthInterval = new DateInterval(beginMonth, endMonth);	
		LocalDate endContract = this.contract.expireContract;
		if(contract.endContract!=null)
			endContract = contract.endContract;
		
		if(DateUtility.isDateIntoInterval(endContract, monthInterval))
			this.contractDescription = "(contratto scaduto in data " + endContract+")";
		else
			this.contractDescription = "";
		
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
				CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
				if(c.getMese(year, month)!=null)
					return c.getMese(year, month).progressivoFinalePositivoMese;
			}
		}
		return 0;
	}
	
}


