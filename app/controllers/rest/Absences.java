package controllers.rest;

import helpers.JsonResponse;

import java.util.List;

import javax.inject.Inject;

import manager.AbsenceManager;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import models.Absence;
import models.Person;

import org.joda.time.LocalDate;

import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import cnr.sync.dto.AbsenceAddedRest;
import cnr.sync.dto.AbsenceRest;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDao;

@With(Resecure.class)
public class Absences extends Controller{
	
	@Inject
	static PersonDao personDao;
	@Inject
	static AbsenceDao absenceDao;
	@Inject
	static AbsenceManager absenceManager;
	@Inject
	static AbsenceTypeDao absenceTypeDao;
	
	@BasicAuth
	public static void absencesInPeriod(String email, LocalDate begin, LocalDate end){
		Person person = personDao.byEmail(email).orNull();
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
	
	@BasicAuth
	public static void insertAbsence(String email, String absenceCode, LocalDate begin, LocalDate end){
		Person person = personDao.byEmail(email).orNull();
		if(person == null){
			JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
					+ "mail cnr che serve per la ricerca.");
		}
		if(begin == null || end == null || begin.isAfter(end)){
			JsonResponse.badRequest("Date non valide");
		}
		List<AbsenceAddedRest> list = Lists.newArrayList();
		try{
			AbsenceInsertReport air = absenceManager.insertAbsence(person, begin, Optional.fromNullable(end), 
					absenceTypeDao.getAbsenceTypeByCode(absenceCode).get(), 
					Optional.<Blob>absent(), Optional.<String>absent());
			for(AbsencesResponse ar : air.getAbsences()){
				AbsenceAddedRest aar = new AbsenceAddedRest();
				aar.absenceCode = ar.getAbsenceCode();
				aar.date = ar.getDate().toString();
				aar.isOK = ar.isInsertSucceeded();
				aar.reason = ar.getWarning();
				list.add(aar);
			}
			renderJSON(list);
		}
		catch(Exception e){
			JsonResponse.badRequest("Errore nei parametri passati al server");
		}
		
		
	}

}
