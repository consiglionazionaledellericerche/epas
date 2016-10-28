package manager.recaps.competence;

import dao.CompetenceDao;
import dao.PersonDao;

import manager.CompetenceManager;

import models.CompetenceCode;
import models.Office;

import javax.inject.Inject;

public class CompetenceRecapFactory {

  private final PersonDao personDao;
  private final CompetenceManager competenceManager;
  private final CompetenceDao competenceDao;
  
  @Inject
  CompetenceRecapFactory(PersonDao personDao, CompetenceManager competenceManager, 
      CompetenceDao competenceDao) {
    this.competenceDao = competenceDao;
    this.competenceManager = competenceManager;
    this.personDao = personDao;
  }
  
  public CompetenceRecap create(Office office, CompetenceCode code, 
      int year, int month) {
    return new CompetenceRecap(personDao, competenceManager, competenceDao, 
        year, month, office, code);
  }
  
}
