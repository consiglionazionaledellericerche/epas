package models.personalMonthSituation;

import java.util.List;

import org.joda.time.LocalDate;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.PersonMonth;

public class Mese {

	public Person person;
	public int workingTime;
	public int qualifica;
	public Mese mesePrecedente;
	public int anno;
	public int mese;

	public int tempoInizializzazione;
	public int progressivoFinaleMese;				//person day
	public int progressivoFinalePositivoMese = 0;	//person day
	public int progressivoFinaleNegativoMese = 0;	//person day
	
	public int progressivoFinalePositivoMesePrint = 0;	//per il template

	public int straordinariMinuti 			 = 0;	//person month
	public int riposiCompensativiMinuti 	 = 0;	//person month
	
	public int progressivoFinaleNegativoMeseImputatoAnnoPassato;
	public int progressivoFinaleNegativoMeseImputatoAnnoCorrente;
	public int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
	
	public int riposiCompensativiMinutiImputatoAnnoPassato;
	public int riposiCompensativiMinutiImputatoAnnoCorrente;
	public int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
	
	public int monteOreAnnoPassato;
	public int monteOreAnnoCorrente;
	
	//public int residuoMesiPrecedentiAnnoCorrente;
	
	public Mese(Mese mesePrecedente, int anno, int mese, Person person, int tempoInizializzazione, boolean febmar)
	{
		//Gennaio
		if(mesePrecedente==null)
		{
			this.person = person;
			this.workingTime = this.person.workingTimeType.workingTimeTypeDays.get(1).workingTime;
			this.qualifica = person.qualification.qualification;
			this.anno = anno;
			this.mese = mese;
			this.mesePrecedente = null;
			this.tempoInizializzazione = tempoInizializzazione;
			this.monteOreAnnoPassato = tempoInizializzazione;
			
			
			setPersonDayInformation();
			setPersonMonthInformation();
			
			
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(this.monteOreAnnoPassato<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.tempoInizializzazione;
				this.monteOreAnnoPassato = 0;
			}
			
			assegnaProgressivoFinaleNegativo();
			assegnaStraordinari();
			assegnaRiposiCompensativi();
			
			this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = this.progressivoFinaleNegativoMeseImputatoAnnoCorrente + this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
			this.riposiCompensativiMinutiImputatoAnnoCorrente = this.riposiCompensativiMinutiImputatoAnnoCorrente + this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
			
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente + this.progressivoFinalePositivoMese;
			return;
		}
		
		//Febbraio / Marzo
		if(febmar)
		{
			this.person = person;
			this.workingTime = this.person.workingTimeType.workingTimeTypeDays.get(1).workingTime;
			this.qualifica = person.qualification.qualification;
			this.anno = anno;
			this.mese = mese;
			this.mesePrecedente = mesePrecedente;
			this.tempoInizializzazione = tempoInizializzazione;
			this.monteOreAnnoPassato = mesePrecedente.monteOreAnnoPassato;
			this.monteOreAnnoCorrente= mesePrecedente.monteOreAnnoCorrente;
			
			setPersonDayInformation();
			setPersonMonthInformation();
			
			/*dopo gennaio non accade più (speriamo)
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(this.monteOreAnnoPassato<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.tempoInizializzazione;
				this.monteOreAnnoPassato = 0;
			}
			*/
			
			assegnaProgressivoFinaleNegativo();
			assegnaStraordinari();
			assegnaRiposiCompensativi();
			
			this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = this.progressivoFinaleNegativoMeseImputatoAnnoCorrente + this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
			this.riposiCompensativiMinutiImputatoAnnoCorrente = this.riposiCompensativiMinutiImputatoAnnoCorrente + this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
			
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente + this.progressivoFinalePositivoMese;
			return;
		}
		
		if(!febmar)
		{
			this.person = person;
			this.workingTime = this.person.workingTimeType.workingTimeTypeDays.get(1).workingTime;
			this.qualifica = person.qualification.qualification;
			this.anno = anno;
			this.mese = mese;
			this.mesePrecedente = mesePrecedente;
			this.tempoInizializzazione = tempoInizializzazione;
			this.monteOreAnnoPassato = mesePrecedente.monteOreAnnoPassato;
			this.monteOreAnnoCorrente= mesePrecedente.monteOreAnnoCorrente;
			
			if(qualifica>3)
				this.monteOreAnnoPassato = 0;
			
			setPersonDayInformation();
			setPersonMonthInformation();
			
			/*dopo gennaio non accade più (speriamo)
			//se il residuo iniziale e' negativo lo tolgo dal residio mensile positivo
			if(this.monteOreAnnoPassato<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.tempoInizializzazione;
				this.monteOreAnnoPassato = 0;
			}
			*/
			
			assegnaProgressivoFinaleNegativo();
			assegnaStraordinari();
			assegnaRiposiCompensativi();
			
			this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = this.progressivoFinaleNegativoMeseImputatoAnnoCorrente + this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
			this.riposiCompensativiMinutiImputatoAnnoCorrente = this.riposiCompensativiMinutiImputatoAnnoCorrente + this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
			
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente + this.progressivoFinalePositivoMese;
			return;
		}
	}
	
	public void setPersonDayInformation()
	{
		LocalDate monthBegin = new LocalDate(this.anno, this.mese, 1);
		
		LocalDate monthEnd = new LocalDate(this.anno, this.mese, 1).dayOfMonth().withMaximumValue();
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
//		if(pdList.get(0) != null)
//			this.progressivoFinaleMese = pdList.get(0).progressive;
//		else
//			this.progressivoFinaleMese = p;
		
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
	
	public void setPersonMonthInformation()
	{
		PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, this.anno, this.mese).first();
		if(pm==null)
			return;

		this.straordinariMinuti = pm.straordinari;

		List<Absence> riposiCompensativi = Absence.find("Select abs from Absence abs, AbsenceType abt, PersonDay pd where abs.personDay = pd and abs.absenceType = abt and abt.code = ? and pd.person = ? "
				+ "and pd.date between ? and ?", "91", this.person, new LocalDate(this.anno, this.mese, 1), new LocalDate(this.anno, this.mese, 1).dayOfMonth().withMaximumValue()).fetch();
		
		this.riposiCompensativiMinuti = riposiCompensativi.size() * this.workingTime;
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
		/*
		if(this.progressivoFinaleNegativoMese < this.progressivoFinalePositivoMese)
		{
			this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.progressivoFinaleNegativoMese;
			this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = this.progressivoFinaleNegativoMese;
			return;
		}
		else
		{
			this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = this.progressivoFinalePositivoMese;
			this.progressivoFinalePositivoMese = 0;
			this.progressivoFinaleNegativoMese = this.progressivoFinaleNegativoMese - this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
		}
		*/
		this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.progressivoFinaleNegativoMese;
		this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = this.progressivoFinaleNegativoMese;
		return;
		
		
		/*
		this.monteOreAnnoPassato = this.monteOreAnnoPassato - this.progressivoFinaleNegativoMese;
		if(this.monteOreAnnoPassato<0)
		{
			this.progressivoFinaleNegativoMeseImputatoAnnoCorrente = -1 * this.monteOreAnnoPassato; //** solo per debug
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente + this.monteOreAnnoPassato;
			this.monteOreAnnoPassato = 0;
			if(this.monteOreAnnoCorrente<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.monteOreAnnoCorrente;
				this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = -1 * this.monteOreAnnoCorrente; //** solo per debug
			}
		}
		this.progressivoFinaleNegativoMeseImputatoAnnoPassato = this.progressivoFinaleNegativoMese - this.progressivoFinaleNegativoMeseImputatoAnnoCorrente - this.progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese; //solo per debug
		*/
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
		/*
		if(this.riposiCompensativiMinuti < this.progressivoFinalePositivoMese)
		{
			this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.riposiCompensativiMinuti;
			this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = this.riposiCompensativiMinuti;
			return;
		}
		else
		{
			this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = this.progressivoFinalePositivoMese;
			this.progressivoFinalePositivoMese = 0;
			this.riposiCompensativiMinuti = this.riposiCompensativiMinuti - this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
		}
		*/
		this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese - this.riposiCompensativiMinuti;
		this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = this.riposiCompensativiMinuti;
		//this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = this.progressivoFinalePositivoMese;
		//this.riposiCompensativiMinuti = this.riposiCompensativiMinuti - this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
		
		
		/*
		this.monteOreAnnoPassato = this.monteOreAnnoPassato - this.riposiCompensativiMinuti;
		if(this.monteOreAnnoPassato<0)
		{
			this.riposiCompensativiMinutiImputatoAnnoCorrente = -1 * this.monteOreAnnoPassato; //** solo per debug
			this.monteOreAnnoCorrente = this.monteOreAnnoCorrente + this.monteOreAnnoPassato;
			this.monteOreAnnoPassato = 0;
			if(this.monteOreAnnoCorrente<0)
			{
				this.progressivoFinalePositivoMese = this.progressivoFinalePositivoMese + this.monteOreAnnoCorrente;
				this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = -1 * this.monteOreAnnoCorrente; //** solo per debug
			}
			
		}
		this.riposiCompensativiMinutiImputatoAnnoPassato = this.riposiCompensativiMinuti - this.riposiCompensativiMinutiImputatoAnnoCorrente - this.riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese; //solo per debug
		*/
	}
	
}


