package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name="parameter_groups")
public class ParameterGroup extends Model{
	
	public String description;
	
	@OneToMany (mappedBy="parameterGroup")
	public List<ConfParameters> parameters;
	 

}
