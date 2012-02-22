package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class GroupOfParameters extends Model{
	
	public String description;
	
	@OneToMany (mappedBy="groupOfParameters")
	public List<ConfParameters> parameters;
	 

}
