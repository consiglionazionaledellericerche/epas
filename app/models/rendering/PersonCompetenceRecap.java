package models.rendering;

import java.util.List;

import javax.inject.Inject;

import models.CompetenceCode;
import models.Person;
import dao.CompetenceCodeDao;

/**
 * 
 * @author dario
 *
 */
public class PersonCompetenceRecap {
	
	@Inject
	private CompetenceCodeDao competenceCodeDao;
	
	public final Person person;
	public final List<CompetenceCode> personActiveCompetence;
	public final List<CompetenceCode> totalCompetenceCode;

	public PersonCompetenceRecap(Person person, List<CompetenceCode> personActiveCompetence){
		this.person = person;
		this.personActiveCompetence = personActiveCompetence;
		this.totalCompetenceCode = competenceCodeDao.getAllCompetenceCode();
	}

	public PersonCompetenceRecap(Person person){
		this.person = person;
		this.personActiveCompetence = person.competenceCode;
		this.totalCompetenceCode = competenceCodeDao.getAllCompetenceCode();
	}

	public List<CompetenceCode> getCompetenceCodeFromPersonList(){
		return this.person.competenceCode;
	}

	public void setCompetenceCodeToPersonList(CompetenceCode code){
		this.personActiveCompetence.add(code);
	}

}
