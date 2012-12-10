package controllers;

import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;


import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Person;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.ShiftTimeTable;
import models.ShiftType;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

/**
 * 
 * @author dario, arianna
 *
 */
public class Shift extends Controller{

	public static void personList(){
		Reperibility.personList();		
	}
	
	public static void find(){
		response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		Long type = Long.parseLong(params.get("type"));
		
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));
		
		List<PersonShiftDay> personShiftDay = PersonShiftDay.find("SELECT psd FROM PersonShiftDay psd WHERE psd.date BETWEEN ? AND ? ORDER BY psd.date", from, to).fetch();
		Logger.debug("Shift find called from %s to %s, found %s reperibility days", from, to, personShiftDay.size());
		
		List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
		ShiftPeriod shiftPeriod = null;
		for(PersonShiftDay psd : personShiftDay){
			if (shiftPeriod == null || !shiftPeriod.person.equals(psd.personShift.person) || !shiftPeriod.end.plusDays(1).equals(psd.date)) {
				shiftPeriod = new ShiftPeriod(psd.personShift.person, psd.date, psd.date, (ShiftType) ShiftType.findById(type), psd.shiftTimeTables);
				shiftPeriods.add(shiftPeriod);
				Logger.trace("Creato nuovo reperibilityPeriod, person=%s, start=%s, end=%s, timetable=%s" , shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftTimeTable);
			} else {
				shiftPeriod.end = psd.date;
				Logger.trace("Aggiornato ShiftPeriod, person=%s, start=%s, end=%s", shiftPeriod.person, shiftPeriod.start, shiftPeriod.end);
			}
		}
		Logger.debug("Find %s shiftPeriods. ShiftPeriods = %s", shiftPeriods.size(), shiftPeriods);
		render(shiftPeriods);
		
	}
	
	public static void update(Long type, Integer year, Integer month, @As(binder=JsonShiftPeriodsBinder.class) ShiftPeriods body){
		Logger.debug("update: Received reperebilityPeriods %s", body);
		
		if (body == null) {
			badRequest();	
		}
		
		ShiftType shiftType = ShiftType.findById(type);
		
		if (shiftType == null) {
			throw new IllegalArgumentException(String.format("ShiftType id = %s doesn't exist", type));			
		}
	}
	
	public static void absence(){
		
	}
}
