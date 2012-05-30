package models;

import java.sql.Blob;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.joda.time.LocalDate;


import play.db.jpa.Model;

@Entity
@Table(name="conf_parameters")
public class ConfParameters extends Model{
	
	public String value;
	
	public String description;
	
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name="parameter_group_id")
	public ParameterGroup parameterGroup;
	
	public Blob param;
}
