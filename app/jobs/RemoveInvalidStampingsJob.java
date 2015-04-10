package jobs;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import manager.PersonDayManager;
import models.Person;
import models.PersonDay;
import models.Stamping;
import play.Logger;
import play.jobs.Job;

public class RemoveInvalidStampingsJob extends Job {
	
	@Inject
	static private PersonDayManager personDayManager;
	@Inject
	static private PersonDayDao personDayDao;
	@Inject
	static private IWrapperFactory wrapperFactory;
	
	final private Person person;
	final private LocalDate begin;
	final private LocalDate end;
	
	public RemoveInvalidStampingsJob(Person person, LocalDate begin, LocalDate end) {
		super();
		this.person = person;
		this.begin = begin;
		this.end = end;
	}
	
	public void doJob(){
		Logger.info("Inizio Job RemoveInvalidStampingsJob per %s,Dal %s al %s",person,begin,end);
		List<PersonDay> persondays = personDayDao.getPersonDayInPeriod(person, begin, Optional.of(end), true);
		for(PersonDay pd : persondays){
			personDayManager.populatePersonDay(wrapperFactory.create(pd));
			for(Stamping stamping : pd.stampings){
				if(!stamping.valid){
					Logger.info("Eliminazione timbratura non valida per %s in data %s : %s",pd.person.fullName(),pd.date, stamping);
					stamping.delete();
				}
			}
		}
		Logger.info("Terminato Job RemoveInvalidStampingsJob per %s,Dal %s al %s",person,begin,end);
	}
}
