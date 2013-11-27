package models.personalMonthSituation;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

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
		public CalcoloSituazioneAnnualePersona(Person person, int year, int initializationTime, LocalDate calcolaFinoA)
		{
			
			//costruisco gennaio
			Mese gennaio = new Mese(null, 2013, 1, person, initializationTime, false, calcolaFinoA);
			
			//costruisco febbraio e marzo
			Mese febbraio = new Mese(gennaio, 2013, 2, person, initializationTime, true, calcolaFinoA);
			Mese marzo = new Mese(febbraio, 2013, 3, person, initializationTime, true, calcolaFinoA);
			
			//gli altri mesi
			Mese aprile 	= new Mese(marzo, 2013, 4, person, initializationTime, false, calcolaFinoA);
			Mese maggio 	= new Mese(aprile, 2013, 5, person, initializationTime, false, calcolaFinoA);
			Mese giugno 	= new Mese(maggio, 2013, 6, person, initializationTime, false, calcolaFinoA);
			Mese luglio 	= new Mese(giugno, 2013, 7, person, initializationTime, false, calcolaFinoA);
			Mese agosto 	= new Mese(luglio, 2013, 8, person, initializationTime, false, calcolaFinoA);
			Mese settembre  = new Mese(agosto, 2013, 9, person, initializationTime, false, calcolaFinoA);
			Mese ottobre 	= new Mese(settembre, 2013, 10, person, initializationTime, false, calcolaFinoA);
			Mese novembre   = new Mese(ottobre, 2013, 11, person, initializationTime, false, calcolaFinoA);
			Mese dicembre   = new Mese(novembre, 2013, 12, person, initializationTime, false, calcolaFinoA);
			
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
