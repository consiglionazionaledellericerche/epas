package models.exports;

import java.util.ArrayList;
import java.util.List;
import models.Competence;

/**
 * La lista di competenze associate alle persone.
 * @author arianna
 */
public class PersonsCompetences {

  public List<Competence> competences = new ArrayList<Competence>();

  public PersonsCompetences(List<Competence> competences) {
    this.competences = competences;
  }
}
