import java.util.Calendar;
import java.util.GregorianCalendar;
import play.Logger;

import models.Person;
import models.PersonMonth;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;

import play.test.Fixtures;
import play.test.UnitTest;

/**
 * 
 * @author dario
 *
 */
public class MonthTest extends UnitTest{

	@Before
	public void loadFixtures() {
		Fixtures.deleteDatabase();
		Fixtures.loadModels("monthTest.yml");
	}
	
	@Test
	public void testWorkingDays(){
		
		LocalDate data = new LocalDate(2011,4,1);
		long id = 1;
		Person person = Person.findById(id);
		PersonMonth pm = new PersonMonth(person, data);
		assertNotNull(person);
		initializeStamping(person);
		int giorniLavoro = pm.getWorkingDays();
		int giorniLavorativi = pm.monthWorkingDays();
		int giorniLavoroInVacanza = pm.workingDaysInHoliday();
		int oreLavoroMensili = pm.timeAtWork();
		int buoniDaRendere = pm.mealTicketToRender();
		System.out.println("I giorni di lavoro sono: " +giorniLavoro);
		System.out.println("I giorni lavorativi invece sono: " +giorniLavorativi);
		System.out.println("Ho lavorato: " +giorniLavoroInVacanza+ " giorni nonostante fosse vacanza");
		System.out.println("Ho lavorato " +oreLavoroMensili+ " minuti in questo mese che corrispondono a "+oreLavoroMensili/60+ " ore e " +oreLavoroMensili%60+ " minuti" );
		System.out.println("Devo restituire: "+buoniDaRendere+ " buoni pasto");
	}
	
	
	
	public void initializeStamping(Person person){
		String causa = new String ("Timbratura di ingresso");
		String causa2 = new String ("Timbratura d'uscita per pranzo");
		String causa3 = new String ("Timbratura di ingresso dopo pausa pranzo");
		String causa4 = new String ("Timbratura di uscita");
		int year = 2011;
		int month = 4;
					
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for(int day = 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++){
			LocalDate data = new LocalDate(year,month,day);
			int giornata = data.getDayOfWeek();
	//		Integer i = new Integer(giornata);
			System.out.println("Giornata ha il valore di: "+giornata);
			Logger.warn("Giornata ha il valore di: "+giornata);
			if(giornata != 6){
				if(giornata != 7){
					Stamping s = new Stamping();
					s.person = person;
					s.stampType = StampType.find("Select s from StampType s where description = ?",causa).first();
					s.way = WayType.in;
					s.date = new LocalDateTime(year,month,day,8,15,0);
					s.save();
					Stamping s1 = new Stamping();
					s1.person = person;				
					s1.stampType = StampType.find("Select s from StampType s where description = ?",causa2).first();
					s1.way = WayType.out;
					s1.date = new LocalDateTime(year,month,day,12,22,0);
					s1.save();
					Stamping s2 = new Stamping();
					s2.date = new LocalDateTime(year,month,day,13,40,0);
					s2.person = person;
					s2.way = WayType.in;
					s2.stampType = StampType.find("Select s from StampType s where description = ?",causa3).first();
					s2.save();
					Stamping s3 = new Stamping();
					s3.date = new LocalDateTime(year,month,day,18,29,0);
					s3.person = person;
					s3.way = WayType.out;
					s3.stampType = StampType.find("Select s from StampType s where description = ?",causa4).first();
					s3.save();	
				}
				else{
					System.out.println("Non scrivo timbratura perchè oggi è sabato o domenica");
					Logger.warn("Non scrivo timbratura perchè oggi è sabato o domenica");
				}
			}
			else{
				System.out.println("Non scrivo timbratura perchè oggi è sabato o domenica");
				Logger.warn("Non scrivo timbratura perchè oggi è sabato o domenica");
			}
			
		}
		
	}
	
	
}
