package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name="valuable_competences")
public class ValuableCompetence extends Model {

	//FIXME tabella usata solo da FromMysql
	
	@ManyToOne
	@JoinColumn(name="person_id")
	public Person person;
	
	public String codicecomp;
	
	public String descrizione;
	
}
