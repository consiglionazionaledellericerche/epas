package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name="web_stamping_address")
public class WebStampingAddress extends Model{
	
	public String webAddressType;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="confParameters_id")
	public ConfParameters confParameters;
}
