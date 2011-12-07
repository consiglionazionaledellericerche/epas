package models;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import play.db.jpa.Model;
/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "codes")
public class Code extends Model{
	
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
	public boolean inactive;
	
	@Column
	public boolean internal;
	
	@Column
	public Date fromDate;
	
	@Column
	public Date toDate;
	
	@Column
	public String qualification;
	
	@Column
	public String group;
	
	@Column
	public int value;
	
	@Column
	public boolean minutesOver;
	
	@Column
	public String descriptionValue;
	
	@Column
	public short quantGiust;
	
	@Column
	public boolean quantMin;
	
	@Column
	public short storage;
	
	@Column
	public boolean recoverable;
	
	@Column
	public int limit;
	
	@Column
	public short gestLim;
	
	@Column
	public String codiceSost;
	
	@Column
	public boolean ignoreStamping;
	
	@Column
	public boolean usoMulti;
	
	@Column
	public boolean tempoBuono;
	
}
