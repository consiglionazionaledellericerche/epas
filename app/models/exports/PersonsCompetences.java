/**
 * 
 */
package models.exports;

import models.Competence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arianna
 *
 */
public class PersonsCompetences {

	public List<Competence> competences = new ArrayList<Competence>();
	
	public PersonsCompetences(List<Competence> competences) {
		this.competences = competences;
	}
}
