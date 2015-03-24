package jobs;

import java.util.List;

import javax.inject.Inject;

import manager.ConsistencyManager;
import models.Person;
import models.User;

import org.apache.commons.mail.EmailException;
import org.joda.time.LocalDate;

import play.Logger;
import play.jobs.Job;
import play.jobs.On;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;

//@On("0 34 15 ? * *")
@SuppressWarnings("rawtypes")
@On("0 0 15 ? * MON,WED,FRI")
public class ExpandableJob extends Job{

	@Inject
	static ConsistencyManager consistencyManager;
	
	@Inject
	private static OfficeDao officeDao;
	
	public void doJob(){
		Logger.info("Start Job expandable");
		
		LocalDate fromDate = LocalDate.now().minusMonths(2);
		LocalDate toDate = LocalDate.now().minusDays(1);
		
		List<Person> personList = PersonDao.list(
				Optional.<String>absent(),
				Sets.newHashSet(officeDao.getAllOffices()), 
						false, 
						fromDate, 
						toDate, 
						true).list();
		
		try {
			consistencyManager.sendMail(personList, fromDate, toDate, "no assenze");
		}
		catch(EmailException e){
			e.printStackTrace();
		}
		
		Logger.info("Concluso Job expandable");	
	}
}
