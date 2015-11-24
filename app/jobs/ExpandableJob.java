package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import dao.OfficeDao;
import dao.PersonDao;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayInTroubleManager;
import models.Person;
import org.apache.commons.mail.EmailException;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

import javax.inject.Inject;
import java.util.List;

//@On("0 34 15 ? * *")
@SuppressWarnings("rawtypes")
@Slf4j
@On("0 0 15 ? * MON,WED,FRI")
public class ExpandableJob extends Job{

	@Inject
	private static PersonDayInTroubleManager personDayInTroubleManager;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	
	private final static String JOBS_CONF = "jobs.active";

	public void doJob(){
		
//		in modo da inibire l'esecuzione dei job in base alla configurazione
		if("false".equals(Play.configuration.getProperty(JOBS_CONF))){
			log.info("ExpandableJob Interrotto. Disattivato dalla configurazione.");
			return;
		}
		
		log.info("Start Job expandable");

		LocalDate fromDate = LocalDate.now().minusMonths(2);
		LocalDate toDate = LocalDate.now().minusDays(1);

		List<Person> personList = personDao.list(
				Optional.<String>absent(),
				Sets.newHashSet(officeDao.getAllOffices()), 
				false, 
				fromDate, 
				toDate, 
				true).list();

		try {
			personDayInTroubleManager.sendMail(personList, fromDate, toDate, "no assenze");
		}
		catch(EmailException e){
			e.printStackTrace();
		}

		log.info("Concluso Job expandable");	
	}
}
