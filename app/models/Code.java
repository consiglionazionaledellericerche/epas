package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.jpa.Model;
/**
 * tabella codici del db mysql, per adesso alcuni campi sono riscritti tali e quali al db vecchio.
 * @author dario
 *
 */
@Entity
@Table(name = "codes")
public class Code extends Model{
	
	public String code;
		
	public String code_att;
		
	public String description;
	
	public boolean inactive;
		
	public boolean internal;
		
	public Date fromDate;
	
	public Date toDate;
		
	public String qualification;
		
	public String groupOf;
		
	public int value;
		
	public boolean minutesOver;
		
	public String descriptionValue;
		
	public short quantGiust;
		
	public boolean quantMin;
		
	public short storage;
		
	public boolean recoverable;
		
	public int limitOf;
		
	public short gestLim;
		
	public String codiceSost;
		
	public boolean ignoreStamping;
		
	public boolean usoMulti;
		
	public boolean tempoBuono;
	
}
