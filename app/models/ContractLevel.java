package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name="contractLevel")
public class ContractLevel extends Model{

	@Required
	public String description;
	
	@OneToMany(mappedBy = "contractLevel")
	public List<Contract> contract;
	
//	@OneToMany(mappedBy="contractLevel")
//	public List<AbsenceType> absenceType;
}
