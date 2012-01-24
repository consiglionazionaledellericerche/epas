package controllers;

import models.MonthRecap;
import models.Person;
import net.sf.oval.constraint.NotNull;

import org.joda.time.LocalDate;

import play.mvc.Before;
import play.mvc.Controller;

public class Stampings extends Controller {

<<<<<<< HEAD
    public static void show(Long id) {
=======
    @Before
    static void checkPerson() {
        if(session.get(Application.PERSON_ID_SESSION_KEY) == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }

    private static void show(@NotNull Long id) {
>>>>>>> 5b2767fc827d089fcf0ffe161d3f8912d1d592f5
    	Person person = Person.findById(id);
    	
    	LocalDate now = new LocalDate();
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
        render(monthRecap);
    }

    public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
}
