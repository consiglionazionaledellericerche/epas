package manager.recaps;

import models.CompetenceCode;
import models.Person;

import java.util.List;

/**
 * @author dario
 */
public class PersonCompetenceRecap {

  public final Person person;
  public final List<CompetenceCode> personActiveCompetence;
  public final List<CompetenceCode> totalCompetenceCode;

  public PersonCompetenceRecap(
      Person person, List<CompetenceCode> personActiveCompetence,
      List<CompetenceCode> codes) {
    this.person = person;
    this.personActiveCompetence = personActiveCompetence;
    this.totalCompetenceCode = codes;
  }

  public PersonCompetenceRecap(Person person, List<CompetenceCode> codes) {
    this.person = person;
    this.personActiveCompetence = person.competenceCode;
    this.totalCompetenceCode = codes;
  }

  public List<CompetenceCode> getCompetenceCodeFromPersonList() {
    return this.person.competenceCode;
  }

  public void setCompetenceCodeToPersonList(CompetenceCode code) {
    this.personActiveCompetence.add(code);
  }

}
