package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;
/**
 * 
 * @author dario
 *
 */

public class AbsenceTypeGroup extends Model{
	@Column
	public String label;
	@Column
	public boolean minutesExcess;
	@Column
	public int buildUp;
	@Column
	public int buildUpLimit;
	@Column
	public int buildUpEdgeBehaviour;
	@Column
	public String equivalentCode;
}
