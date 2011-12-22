package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.jpa.Model;

/**
 * tabella di decodifica dei codici di competenza
 * @author dario
 *
 */
@Entity
@Table(name= "competence_code")
public class CompetenceCode extends Model{
	
	@OneToMany(mappedBy="competenceCode")
	public List<Competence> competence;

	public String description;
	
	public boolean inactive;
}
