package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * tabella di decodifica dei codici di competenza
 * 
 * @author dario
 *
 */
@Entity
@Table(name= "competence_codes")
public class CompetenceCode extends Model {
	
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
	
	@Required
	public boolean inactive = false;
	
	@Override
	public String toString() {
		return String.format("CompetenceCode[%d] - description = %s", id, description);
	}
}
