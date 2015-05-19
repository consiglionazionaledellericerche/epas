package jobs;

import groovy.util.logging.Slf4j;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Office;
import models.Person;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import dao.OfficeDao;
import dao.PersonDao;
import dto.DepartmentDTO;
import dto.PersonRest;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.On;
import play.libs.WS;
import play.libs.WS.HttpResponse;

@On("0 10 6 ? * MON")
public class CheckCnrEmailJob extends Job{

	@Inject
	static OfficeDao officeDao; 
	@Inject 
	static PersonDao personDao;

	public void doJob() {
		if (Office.count() == 0 || Person.count() == 0)
			return;
		List<Office> helpList = officeDao.getAllOffices();
		List<Office> officeList = Lists.newArrayList();
		for(Office office : helpList){
			if(office.code != null)
				officeList.add(office);
		}

		String url = Play.configuration.getProperty("people.rest");
		String perseoUrl = Play.configuration.getProperty("perseo.department");
		for(Office office : officeList){
			perseoUrl = perseoUrl+office.code.toString();
			HttpResponse perseoResponse = WS.url(perseoUrl).get();
			Gson gson = new Gson();

			DepartmentDTO dep = gson.fromJson(perseoResponse.getJson(),DepartmentDTO.class);
			HttpResponse response = WS.url(url+dep.code)
					.authenticate(Play.configuration.getProperty("people.rest.user"), 
							Play.configuration.getProperty("people.rest.password")).get();

			List<PersonRest> people = gson.fromJson(response.getJson().toString(),
					new TypeToken<ArrayList<PersonRest>>() {}.getType());
			for(PersonRest pr : people){
				if(pr.matricola == null){
					Logger.info("Non esiste matricola per %s %s", pr.nome, pr.cognome);
				}
				else{
					Person person = personDao.getPersonByNumber(pr.matricola);
					if(person != null){
						person.cnr_email = pr.email_comunicazioni;
						//person.iId = new Integer(pr.uid);
						person.save();
						Logger.info("Salvata la mail cnr per %s %s", person.name, person.surname);
					}
					else{
						Logger.info("La persona %s %s non è presente in anagrafica", pr.nome, pr.cognome);
					}
				}
				
			}
			perseoUrl = Play.configuration.getProperty("perseo.department");

		}		

	}
}
