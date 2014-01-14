package models.personalMonthSituation;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.InitializationTime;
import models.Person;

public class CalcoloSituazioneAnnualePersona {

		public List<Mese> mesi;
		
		/**
		 * 
		 * @param person
		 * @param year
		 * @param initializationTime
		 * @param calcolaFinoA 
		 */
		public CalcoloSituazioneAnnualePersona(Person person, int year, LocalDate calcolaFinoA)
		{
			int initializationTimeMinute = 0;
			if(year==2014)
			{
				CalcoloSituazioneAnnualePersona csap2013 = new CalcoloSituazioneAnnualePersona(person, 2013, null);
				initializationTimeMinute = csap2013.getMese(2013, 12).monteOreAnnoCorrente + csap2013.getMese(2013, 12).monteOreAnnoPassato;
			}
			else
			{
				LocalDate beginYear = new LocalDate(year, 1, 1);
				InitializationTime initializationTime = InitializationTime.find("Select i from InitializationTime i where i.person = ? and i.date = ?" , person, beginYear).first();
				if(initializationTime != null)
					initializationTimeMinute = initializationTime.residualMinutesPastYear;
			}
			
			//costruisco gennaio
			Mese gennaio = new Mese(null, year, 1, person, initializationTimeMinute, false, calcolaFinoA);
			
			//costruisco febbraio e marzo
			Mese febbraio = new Mese(gennaio, year, 2, person, initializationTimeMinute, true, calcolaFinoA);
			Mese marzo = new Mese(febbraio, year, 3, person, initializationTimeMinute, true, calcolaFinoA);
			
			//gli altri mesi
			Mese aprile 	= new Mese(marzo, year, 4, person, initializationTimeMinute, false, calcolaFinoA);
			Mese maggio 	= new Mese(aprile, year, 5, person, initializationTimeMinute, false, calcolaFinoA);
			Mese giugno 	= new Mese(maggio, year, 6, person, initializationTimeMinute, false, calcolaFinoA);
			Mese luglio 	= new Mese(giugno, year, 7, person, initializationTimeMinute, false, calcolaFinoA);
			Mese agosto 	= new Mese(luglio, year, 8, person, initializationTimeMinute, false, calcolaFinoA);
			Mese settembre  = new Mese(agosto, year, 9, person, initializationTimeMinute, false, calcolaFinoA);
			Mese ottobre 	= new Mese(settembre, year, 10, person, initializationTimeMinute, false, calcolaFinoA);
			Mese novembre   = new Mese(ottobre, year, 11, person, initializationTimeMinute, false, calcolaFinoA);
			Mese dicembre   = new Mese(novembre, year, 12, person, initializationTimeMinute, false, calcolaFinoA);
			
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
