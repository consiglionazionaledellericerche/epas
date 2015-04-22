package controllers.rest;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import javax.inject.Inject;

import manager.PersonDayManager;
import models.Absence;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.mvc.With;
import controllers.RequestInit;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Resecure.NoCheck;
import dao.AbsenceDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dto.DayRecap;

@With(Resecure.class)
public class Persons extends Controller{
	
	@Inject
	static PersonDao personDao;
	@Inject 
	static PersonDayManager personDayManager;
	@Inject
	static IWrapperFactory wrapperFactory;
	@Inject
	static AbsenceDao absenceDao;
	
	@BasicAuth
	public static void days(String email,LocalDate start,LocalDate end){

		Person person = personDao.getPersonByEmail(email);
		if(person == null){
//			TODO return not found
		}
		if(start == null || end == null){
//			TODO return Bad request
		}

		List<DayRecap> personDays = Lists.newArrayList();

		personDays = FluentIterable.from(personDao.getPersonDayIntoInterval(
				 person,new DateInterval(start, end) , false))
				.transform(	new	Function<PersonDay, DayRecap>(){
			@Override
			public DayRecap apply(PersonDay personday){
				DayRecap dayRecap = new DayRecap();
				dayRecap.workingMinutes = personDayManager.workingMinutes(wrapperFactory.create(personday));
				dayRecap.date = personday.date.toString();
				dayRecap.mission = personDayManager.isOnMission(personday);
				return dayRecap;
			}}).toList();

		renderJSON(personDays);
	}
	
	@BasicAuth
	public static void missions(String email, LocalDate start, LocalDate end, boolean forAttachment){
		Person person = personDao.getPersonByEmail(email);
		List<DayRecap> personDays = Lists.newArrayList();
		if(person != null){
			
			personDays = FluentIterable.from(
					absenceDao.getAbsencesInPeriod(Optional.fromNullable(person), start, Optional.fromNullable(end), forAttachment))
					.transform(new	Function<Absence, DayRecap>(){
				@Override
				public DayRecap apply(Absence absence){
					DayRecap dayRecap = new DayRecap();
					dayRecap.workingMinutes = 0;
					dayRecap.date = absence.personDay.date.toString();
					if(personDayManager.isOnMission(absence.personDay)){						
						dayRecap.mission = true;						
					}				
					else{						
						dayRecap.mission = false;
					}
					return dayRecap;
				}}).toList();
			}
			renderJSON(personDays);
	}
}
