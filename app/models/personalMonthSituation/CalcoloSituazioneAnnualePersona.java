package models.personalMonthSituation;

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
		 * @param person
		 * @param year
		 * @param initializationTime
		 * @param calcolaFinoA valorizzare questo campo per fotografare la situazione residuale in un certo momento 
		 *   (ad esempio se si vuole verificare la possibilità di prendere riposo compensativo in un determinato giorno). 
		 *   Null se si desidera la situazione residuale a oggi. 
		 */
		public CalcoloSituazioneAnnualePersona(Contract contract, int year, LocalDate calcolaFinoA)
		{
			Person person = contract.person;
			//get initialization time per l'anno
			
			//NUOVO ALGORITMO
			int initializationTimeMinuteNew;
			ContractYearRecap recapPreviousYear = contract.getYearRecap(year-1);
			if(recapPreviousYear==null)
				initializationTimeMinuteNew = 0;
			else
				initializationTimeMinuteNew = recapPreviousYear.remainingMinutesCurrentYear + recapPreviousYear.remainingMinutesLastYear;
			
			/*
			//VECCHIO ALGORITMO
			int initializationTimeMinute = 0;
			if(year==2014)
			{
				CalcoloSituazioneAnnualePersona csap2013 = new CalcoloSituazioneAnnualePersona(contract, 2013, null);
				initializationTimeMinute = csap2013.getMese(2013, 12).monteOreAnnoCorrente + csap2013.getMese(2013, 12).monteOreAnnoPassato;
			}
			else
			{
				LocalDate beginYear = new LocalDate(year, 1, 1);
				InitializationTime initializationTime = InitializationTime.find("Select i from InitializationTime i where i.person = ? and i.date = ?" , person, beginYear).first();
				if(initializationTime != null)
					initializationTimeMinute = initializationTime.residualMinutesPastYear;
			}
			
			//CONFRONTO
			if(initializationTimeMinute==initializationTimeMinuteNew)
				Logger.debug("OK per %s (%s %s)", year, person.name, person.surname);
			else
				Logger.debug("AIUTOOOOOO per %s (%s %s) nuovo=%s vecchio=%s", year, person.name, person.surname
						, DateUtility.fromMinuteToHourMinute(initializationTimeMinuteNew), DateUtility.fromMinuteToHourMinute(initializationTimeMinute));
			
			
			*/
			
			
			//costruisco gennaio
			Mese gennaio = new Mese(null, year, 1, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			
			//costruisco febbraio e marzo
			Mese febbraio = new Mese(gennaio, year, 2, contract, initializationTimeMinuteNew, true, calcolaFinoA);
			Mese marzo = new Mese(febbraio, year, 3, contract, initializationTimeMinuteNew, true, calcolaFinoA);
			
			//gli altri mesi
			Mese aprile 	= new Mese(marzo, year, 4, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese maggio 	= new Mese(aprile, year, 5, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese giugno 	= new Mese(maggio, year, 6, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese luglio 	= new Mese(giugno, year, 7, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese agosto 	= new Mese(luglio, year, 8, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese settembre  = new Mese(agosto, year, 9, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese ottobre 	= new Mese(settembre, year, 10, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese novembre   = new Mese(ottobre, year, 11, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			Mese dicembre   = new Mese(novembre, year, 12, contract, initializationTimeMinuteNew, false, calcolaFinoA);
			
			this.mesi = new ArrayList<Mese>();
			this.mesi.add(gennaio);
			this.mesi.add(febbraio);
			this.mesi.add(marzo);
			this.mesi.add(aprile);
			this.mesi.add(maggio);
			this.mesi.add(giugno);
			this.mesi.add(luglio);
			this.mesi.add(agosto);
			this.mesi.add(settembre);
			this.mesi.add(ottobre);
			this.mesi.add(novembre);
			this.mesi.add(dicembre);

		}
		
		public Mese getMese(int year, int month){
			return this.mesi.get(month-1);
		}
		
		
}
