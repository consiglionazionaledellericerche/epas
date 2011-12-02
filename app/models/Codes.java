package models;

import java.util.Set;

import javax.persistence.Column;


import org.joda.time.LocalDate;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
public class Codes extends Model{
	
	/**
	 * tabella codici del db mysql, per adesso alcuni campi sono riscritti tali e quali al db vecchio.
	 * 
	 */
	
	@Column
	public String code;
	
	@Column
	public String code_att;
	
	@Column
	public String description;
	
	@Column
	public byte inactive;
	
	@Column
	public byte internal;
	
	@Column
	public LocalDate fromDate;
	
	@Column
	public LocalDate toDate;
	
	@Column
	public Set qualification;
	
	@Column
	public int value;
	
	@Column
	public byte minutesOver;
	
	@Column
	public String descriptionValue;
	
	@Column
	public byte quantGiust;
	
	@Column
	public byte quantMin;
	
	@Column
	public byte storage;
	
	@Column
	public byte recoverable;
	
	@Column
	public int limit;
	
	@Column
	public byte gestLim;
	
	@Column
	public String codiceSost;
	
	@Column
	public byte ignoreStamping;
	
	@Column
	public byte usoMulti;
	
	@Column
	public byte tempoBuono;
	
}
