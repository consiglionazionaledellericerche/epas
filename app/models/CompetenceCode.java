package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;
import play.data.validation.Required;


/**
 * tabella di decodifica dei codici di competenza
 * 
 * @author dario
 *
 */
@Entity
@Table(name= "competence_codes")
public class CompetenceCode extends BaseModel {
	
	private static final long serialVersionUID = 9211205948423608460L;

	@OneToMany(mappedBy="competenceCode")
	public List<Competence> competence;
	
	@ManyToMany(mappedBy = "competenceCode")
	public List<Person> persons;
	
	@Required
	public String code;
	
	@Column
	public String codeToPresence;

	@Required
	public String description;
	
//	@Required
//	public boolean inactive = false;
	
	@Override
	public String toString() {
		return String.format("CompetenceCode[%d] - description = %s", id, description);
	}
	

	/**
	 * 
	 * @param month
	 * @param year
	 * @return il totale per quel mese e quell'anno di ore/giorni relativi a quel codice competenza
	 */
	public int totalFromCompetenceCode(int month, int year, Long officeId){
		
		Office office = Office.findById(officeId);
		
		int totale = 0;
		
		List<Competence> compList = Competence.find("Select comp from Competence comp where comp.competenceCode = ? " +
				"and comp.month = ? and comp.year = ? and comp.person.office = ?", this, month, year, office).fetch();
		for(Competence comp : compList){
			totale = totale+comp.valueApproved;
		}
		return totale;
	}
}
