package models.personalMonthSituation;

import java.util.List;

import org.joda.time.LocalDate;

import play.Logger;
import models.Absence;
import models.Competence;
import models.Person;
import models.PersonDay;

public class Mese {

	public Person person;
	public int qualifica;
	public boolean possibileUtilizzareResiduoAnnoPrecedente = true;
	public Mese mesePrecedente;
	public int anno;
	public int mese;

	public int tempoInizializzazione;
	public int progressivoFinaleMese;				//person day
	public int progressivoFinalePositivoMese = 0;	//person day
	public int progressivoFinaleNegativoMese = 0;	//person day
	
	public int progressivoFinalePositivoMesePrint = 0;	//per il template

	public int straordinariMinuti 			= 0;	//competences
	public int straordinariMinutiS1Print	= 0;	//per il template
	public int straordinariMinutiS2Print	= 0;	//per il template
	public int straordinariMinutiS3Print	= 0;	//per il template
	public int riposiCompensativiMinuti 	= 0;	//absences
	
	public int progressivoFinaleNegativoMeseImputatoAnnoPassato;
	public int progressivoFinaleNegativoMeseImputatoAnnoCorrente;
	public int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
	
	public int riposiCompensativiMinutiImputatoAnnoPassato;
	public int riposiCompensativiMinutiImputatoAnnoCorrente;
	public int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
	
	public int monteOreAnnoPassato;
	public int monteOreAnnoCorrente;
	public int numeroRiposiCompensativi;
	
	/**
	 * Costruisce un oggetto mese con tutte le informazioni necessarie al calcolo della situazione residuo annuale della persona.
	 * Visibile solo all'interno del package models.personalMonthSituation.
	 * @param mesePrecedente
	 * @param anno
	 * @param mese
	 * @param person
	 * @param tempoInizializzazione
	 * @param febmar
	 * @param calcolaFinoA
	 */
	protected Mese(Mese mesePrecedente, int anno, int mese, Person person, int tempoInizializzazione, boolean febmar, LocalDate calcolaFinoA)
	{
		
		this.person = person;
		this.qualifica = person.qualification.qualification;
		this.anno = anno;
		this.mese = mese;
		
		//Gennaio
		if(mesePrecedente==null)
		{
			this.mesePrecedente = null;
			this.tempoInizializzazione = tempoInizializzazione;
			this.monteOreAnnoPassato = tempoInizializzazione;
			
			setPersonDayInformation(calcolaFinoA);
			setPersonMonthInformation(calcolaFinoA);
			
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(this.monteOreAnnoPassato<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.tempoInizializzazione;
				this.monteOreAnnoPassato = 0;
			}
		}
		
		//Febbraio / Marzo
		else if(febmar)
		{
			this.mesePrecedente = mesePrecedente;
			this.tempoInizializzazione = tempoInizializzazione;
			this.monteOreAnnoPassato = mesePrecedente.monteOreAnnoPassato;
			this.monteOreAnnoCorrente= mesePrecedente.monteOreAnnoCorrente;
			
			setPersonDayInformation(calcolaFinoA);
			setPersonMonthInformation(calcolaFinoA);

		}
		
		// Aprile -> Dicembre
		else if(!febmar)
		{
			this.mesePrecedente = mesePrecedente;
			
			this.tempoInizializzazione = tempoInizializzazione;
			this.monteOreAnnoPassato = mesePrecedente.monteOreAnnoPassato;
			this.monteOreAnnoCorrente= mesePrecedente.monteOreAnnoCorrente;
			
			if(qualifica>3)
			{
				this.possibileUtilizzareResiduoAnnoPrecedente = false;
				this.monteOreAnnoPassato = 0;
				this.tempoInizializzazione = 0;
				
			}
			
			setPersonDayInformation(calcolaFinoA);
			setPersonMonthInformation(calcolaFinoA);
			
		}
		

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
	 * @param calcolaFinoA
	 */
	public void setPersonDayInformation(LocalDate calcolaFinoA)
	{
		LocalDate monthBegin = new LocalDate(this.anno, this.mese, 1);
		LocalDate monthEnd = new LocalDate(this.anno, this.mese, 1).dayOfMonth().withMaximumValue();
		if(calcolaFinoA!=null && monthEnd.isAfter(calcolaFinoA))
			monthEnd = calcolaFinoA;
		
		if(new LocalDate().isBefore(monthEnd))
			monthEnd = new LocalDate().minusDays(1);
		
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date desc",
				this.person, monthBegin, monthEnd).fetch();
		
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

		}
		this.progressivoFinaleNegativoMese = this.progressivoFinaleNegativoMese*-1;
	
		this.progressivoFinalePositivoMesePrint = this.progressivoFinalePositivoMese;
	}
	
	/**
	 * 
	 * @param calcolaFinoA, la data fino alla quale cercare riposi compensativi gia' assegnati
	 */
	public void setPersonMonthInformation(LocalDate calcolaFinoA)
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
		
		//intervallo per calcolare i riposi compensativi
		LocalDate monthBegin = new LocalDate(this.anno, this.mese, 1);
		LocalDate monthEnd = new LocalDate(new LocalDate(this.anno, this.mese, 1).dayOfMonth().withMaximumValue());
		if(calcolaFinoA!=null && monthEnd.isAfter(calcolaFinoA))
			monthEnd = calcolaFinoA;

		//con questo si controlla la richiesta di un riposo compensativo in un mese successivo al mese attuale.
		//In questo modo il riposo compensativo viene conteggiato sul residuo del mese attualmente in essere.
		//es.: sono a febbraio e prendo un riposo compensativo per il 5 marzo. Nel "riepilogo ore di lavoro anno corrente" vedrò visualizzato
		//il numero di riposi compensativi presi sia nel mese di febbraio che nel mese di marzo.
		if(new LocalDate().getMonthOfYear()==this.mese)
			monthEnd = new LocalDate(this.anno, this.mese+1,1).dayOfMonth().withMaximumValue();
		
		List<Absence> riposiCompensativi = Absence.find("Select abs from Absence abs, AbsenceType abt, PersonDay pd where abs.personDay = pd and abs.absenceType = abt and abt.code = ? and pd.person = ? "
				+ "and pd.date between ? and ?", "91", this.person, monthBegin, monthEnd).fetch();
		this.riposiCompensativiMinuti = 0;
		this.numeroRiposiCompensativi = 0;
		for(Absence abs : riposiCompensativi){
			this.riposiCompensativiMinuti = this.riposiCompensativiMinuti + this.person.getWorkingTimeType(abs.personDay.date).getWorkingTimeTypeDayFromDayOfWeek(abs.personDay.date.getDayOfWeek()).workingTime;
			this.numeroRiposiCompensativi++;
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
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return il valore di quanti minuti positivi sono stati fatti da quella persona in quel mese/anno
	 */
	public static Integer positiveResidualInMonth(Person person, int year, int month){
		CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(person, year, null);
		Mese mese = c.getMese(year, month);
		return mese.progressivoFinalePositivoMese;
	}
}


