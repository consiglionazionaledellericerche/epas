package controllers;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import models.PersonMonth;
import play.mvc.Controller;

public class Clocks extends Controller{

	public static void show(Integer month, Integer year){
		LocalDate data = new LocalDate();
		render(data);
	}
	
	/**
	 * 
	 * @param personId. Con questo metodo si permette l'inserimento della timbratura per la persona contrassegnata da id personId.
	 */
	public static void insertStamping(Long personId){
		LocalDateTime ldt = new LocalDateTime();
	}
}
