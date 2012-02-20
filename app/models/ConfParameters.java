package models;

import java.sql.Blob;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.joda.time.LocalDate;


import play.db.jpa.Model;

@Entity
public class ConfParameters extends Model{
	
	public String value;
	
	public String description;
	
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name="groupOfParameters_id")
	public GroupOfParameters groupOfParameters;
	
	public Blob param;
}
