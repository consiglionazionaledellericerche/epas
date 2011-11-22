package models;

import java.util.Date;

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
public class PersoneDate extends Model{
	@Column
	public Date dataInizio;
	@Column
	public Date dataFine;
	@Column
	public String note;
	@Column
	public byte continua;
	@Column
	public byte firma;
	@Column
	public byte presenzaDefault;
	
}
