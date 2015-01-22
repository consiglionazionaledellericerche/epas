package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;


/**
 * 
 * @author dario
 *
 */

@Entity
@Table(name = "total_overtime")
public class TotalOvertime extends BaseModel{

	private static final long serialVersionUID = 468974629639837568L;

	@Required
	//  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	public Integer numberOfHours;
	
	public Integer year;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "office_id")
	public Office office;
}
