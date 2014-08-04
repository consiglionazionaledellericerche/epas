package controllers;

import java.util.List;

import javax.inject.Inject;

import models.Stamping;
import play.mvc.Controller;
import dao.HistoryValue;
import dao.PersonDayHistoryDao;

/**
 * @author marco
 *
 */
//@With(Resecure.class)
public class PersonDayHistory extends Controller {
	
	@Inject
	static PersonDayHistoryDao personDayHistoryDao;
	
	public static void stampings(long personDayId) {
		List<HistoryValue<Stamping>> results = personDayHistoryDao
				.stampings(personDayId);
		render(results);
	}
}
