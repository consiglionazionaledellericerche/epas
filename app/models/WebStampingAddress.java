package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;



/**
 * 
 * @author dario
 *
 */
@Entity
@Audited
@Table(name="web_stamping_address")
public class WebStampingAddress extends BaseModel{
	
	public String webAddressType;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="confParameters_id")
	public Configuration confParameters;
}
