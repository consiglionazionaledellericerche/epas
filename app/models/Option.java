package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 * questa classe identifica l'insieme delle opzioni da utilizzare per la costruzione dell'orario 
 * attraverso giorni particolari. Alcuni dei campi sono stati riportati con il nome in italiano con cui erano stati 
 * costruiti sul db mysql poiché non sono riuscito a trovare una giusta e corretta traduzione.
 */
@Entity
@Table(name = "options")
public class Option extends Model{

	@Column
	public boolean patronDay;
	
	@Column
	public boolean patronMonth;
	
	@Column
	public boolean expiredVacationDay;
	
	@Column
	public boolean expiredVacationMonth;
	
	@Column
	public String recoveryAp;
	
	@Column
	public boolean recoveryMonth;
	
	@Column
	public String vacationTypeP;
	
	@Column
	public String vacationType;
	
	@Column
	public String tipo_permieg;
	
	@Column
	public String otherHeadOffice;
	
	@Column
	public String tipo_ferie_gen;
	
	@Column
	public boolean adjustRange;
	
	@Column
	public boolean adjustRangeDay;
	
	@Column
	public boolean autoRange;
	
	@Column
	public boolean EasterChristmas;
	
	@Column
	public Date date;
	
}
