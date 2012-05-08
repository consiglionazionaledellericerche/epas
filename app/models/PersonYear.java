package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 * questa classe rappresenta il residuo annuale relativo a una certa persona per quel che riguarda le ore fatte in più (o in meno)
 * 
 */
@Audited
@Entity
@Table(name="person_year")
public class PersonYear extends Model{
	
	@ManyToOne
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	@Column
	public int remainingDays;
	@Column
	public int remainingHours;
	
}
