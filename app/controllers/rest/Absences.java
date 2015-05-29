package controllers.rest;

import java.util.List;

import helpers.JsonResponse;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import cnr.sync.dto.AbsenceRest;
import models.Absence;
import models.Person;
import dao.AbsenceDao;
import dao.PersonDao;
import play.mvc.Controller;
import play.mvc.With;

@With(Resecure.class)
public class Absences extends Controller{
	
	@Inject
	static PersonDao personDao;
	@Inject
	static AbsenceDao absenceDao;
	
	@BasicAuth
	public static void absencesInPeriod(String email, LocalDate begin, LocalDate end){
		Person person = personDao.getPersonByEmail(email);
		if(person == null){
			JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
					+ "mail cnr che serve per la ricerca.");
		}
		if(begin == null || end == null || begin.isAfter(end)){
			JsonResponse.badRequest("Date non valide");
		}
		List<AbsenceRest> absences = FluentIterable.from(absenceDao.getAbsencesInPeriod(
				Optional.fromNullable(person), begin, Optional.fromNullable(end), false))
				.transform(new Function<Absence, AbsenceRest>(){
					@Override
					public AbsenceRest apply(Absence absence){
						AbsenceRest ar = new AbsenceRest();
						ar.absenceCode = absence.absenceType.code;
						ar.description = absence.absenceType.description;
						ar.date = absence.personDay.date.toString();
						ar.name = absence.personDay.person.name;
						ar.surname = absence.personDay.person.surname;
						return ar;
					}
				}).toList();
		renderJSON(absences);
	}
	
	
	

}
