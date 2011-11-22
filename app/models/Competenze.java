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

public class Competenze extends Model{
	
	@Column
	public byte mese;
	@Column
	public short anno;
	@Column
	public String codice;
	@Column
	public int valore;
}
