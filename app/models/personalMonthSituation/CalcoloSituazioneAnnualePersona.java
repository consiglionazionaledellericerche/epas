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
			InitializationTime initializationTime = InitializationTime.find("Select i from InitializationTime i where i.person = ?" , person).first();
			if(initializationTime != null)
				initializationTimeMinute = initializationTime.residualMinutesPastYear;
			
			//costruisco gennaio
			Mese gennaio = new Mese(null, 2013, 1, person, initializationTimeMinute, false, calcolaFinoA);
			
			//costruisco febbraio e marzo
			Mese febbraio = new Mese(gennaio, 2013, 2, person, initializationTimeMinute, true, calcolaFinoA);
			Mese marzo = new Mese(febbraio, 2013, 3, person, initializationTimeMinute, true, calcolaFinoA);
			
			//gli altri mesi
			Mese aprile 	= new Mese(marzo, 2013, 4, person, initializationTimeMinute, false, calcolaFinoA);
			Mese maggio 	= new Mese(aprile, 2013, 5, person, initializationTimeMinute, false, calcolaFinoA);
			Mese giugno 	= new Mese(maggio, 2013, 6, person, initializationTimeMinute, false, calcolaFinoA);
			Mese luglio 	= new Mese(giugno, 2013, 7, person, initializationTimeMinute, false, calcolaFinoA);
			Mese agosto 	= new Mese(luglio, 2013, 8, person, initializationTimeMinute, false, calcolaFinoA);
			Mese settembre  = new Mese(agosto, 2013, 9, person, initializationTimeMinute, false, calcolaFinoA);
			Mese ottobre 	= new Mese(settembre, 2013, 10, person, initializationTimeMinute, false, calcolaFinoA);
			Mese novembre   = new Mese(ottobre, 2013, 11, person, initializationTimeMinute, false, calcolaFinoA);
			Mese dicembre   = new Mese(novembre, 2013, 12, person, initializationTimeMinute, false, calcolaFinoA);
			
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
