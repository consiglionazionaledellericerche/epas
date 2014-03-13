package models.personalMonthSituation;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import play.Logger;
import models.Contract;
import models.ContractYearRecap;
import models.InitializationTime;
import models.Person;

public class CalcoloSituazioneAnnualePersona {

		public List<Mese> mesi;
		
		/**
		 * Costruisce la situazione annuale residuale della persona.
		 * @param contract
		 * @param year
		 * @param initializationTime
		 * @param calcolaFinoA valorizzare questo campo per fotografare la situazione residuale in un certo momento 
		 *   (ad esempio se si vuole verificare la possibilità di prendere riposo compensativo in un determinato giorno). 
		 *   Null se si desidera la situazione residuale a oggi. 
		 */
		public CalcoloSituazioneAnnualePersona(Contract contract, int year, LocalDate calcolaFinoA)
		{
			int firstMonthToCompute = 1;
			LocalDate firstDayInDatabase = new LocalDate(year,1,1);
			int initMonteOreAnnoPassato = 0;
			int initMonteOreAnnoCorrente = 0;
			
			if(contract==null)
			{
				return;
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

			this.mesi = new ArrayList<Mese>();
			Mese previous = null;
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
				//Calcolo i dati del DataBase dai quali prendere le informazioni non presenti in inizializzazione
				LocalDate monthBegin = new LocalDate(year, actualMonth, 1);
				LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
				if(new LocalDate().isBefore(monthEnd))
					monthEnd = new LocalDate().minusDays(1);
				DateInterval monthInterval = new DateInterval(monthBegin, monthEnd);
				DateInterval requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
				DateInterval contractInterval = contract.getContractDateInterval();
				DateInterval validData = DateUtility.intervalIntersection(monthInterval, requestInterval);
				validData = DateUtility.intervalIntersection(validData, contractInterval);
	
				//Costruisco l'oggetto
				Mese mese = new Mese(previous, year, actualMonth, contract, initMonteOreAnnoPassato, initMonteOreAnnoCorrente, validData);
				this.mesi.add(mese);
				previous = mese;
				actualMonth++;	
			}
		}
		
		public Mese getMese(int year, int month){
			if(this.mesi==null)
				return null;
			for(Mese mese : this.mesi)
				if(mese.mese==month)
					return mese;
			return null;
		}
		
			
		
}
