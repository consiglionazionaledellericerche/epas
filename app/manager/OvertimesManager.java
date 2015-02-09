package manager;

import play.Logger;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonHourForOvertime;
import models.exports.PersonsCompetences;
import models.exports.PersonsList;

import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.CompetenceDao;

public class OvertimesManager {

	/**
	 * 
	 * @param body
	 * @param code
	 * @param year
	 * @param month
	 * @return la tabella contenente la struttura di persona-reason della competenza-codice competenza
	 */
	public static Table<String, String, Integer> buildMonthForExport(PersonsList body, CompetenceCode code, int year, int month){
		Table<String, String, Integer> overtimesMonth = TreeBasedTable.<String, String, Integer>create();
		
		for (Person person : body.persons) {
			Optional<Competence> competence = CompetenceDao.getCompetence(person, year, month, code);
			Logger.debug("find  Competence %s per person=%s, year=%s, month=%s, competenceCode=%s", competence, person, year, month, code);	

			if ((competence.isPresent()) && (competence.get().valueApproved != 0)) {
				
				overtimesMonth.put(person.surname + " " + person.name, competence.get().reason != null ? competence.get().reason : "", competence.get().valueApproved);
				Logger.debug("Inserita riga person=%s reason=%s and  valueApproved=%s", person, competence.get().reason, competence.get().valueApproved);
			} 
		}		
		
		return overtimesMonth;
	}
	
	
	/**
	 * 
	 * @param body
	 * @param year
	 * @param month
	 */
	public static void setRequestedOvertime(PersonsCompetences body, int year, int month){
		for (Competence competence : body.competences) {
			Optional<Competence> oldCompetence = CompetenceDao.getCompetence(competence.person, year, month, competence.competenceCode);
			if (oldCompetence.isPresent()) {
				// update the requested hours
				oldCompetence.get().setValueApproved(competence.getValueApproved(), competence.getReason());
				oldCompetence.get().save();
				
				Logger.debug("Aggiornata competenza %s", oldCompetence);
			} else {
				// insert a new competence with the requested hours an reason
				competence.setYear(year);
				competence.setMonth(month);
				competence.save();
				
				Logger.debug("Creata competenza %s", competence);
			}
		}
	}
	
	/**
	 * setta il quantitativo orario (hours) a disposizione del responsabile (person)
	 * @param person
	 * @param hours
	 */
	public static void setSupervisorOvertime(Person person, Integer hours){
		PersonHourForOvertime personHourForOvertime = CompetenceDao.getPersonHourForOvertime(person);
		if (personHourForOvertime == null) {
			personHourForOvertime = new PersonHourForOvertime(person, hours);
			Logger.debug("Created  PersonHourForOvertime with persons %s and  hours=%s", person.name, hours);
		} else {
			personHourForOvertime.setNumberOfHourForOvertime(hours);
			Logger.debug("Updated  PersonHourForOvertime of persons %s with hours=%s", person.name, hours);
		}
		personHourForOvertime.save();
	}
}
