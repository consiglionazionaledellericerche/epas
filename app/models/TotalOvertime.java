package models;

import models.base.BaseModel;
import org.joda.time.LocalDate;
import play.data.validation.Required;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


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

	public LocalDate date;
	
	public Integer numberOfHours;
	
	public Integer year;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "office_id")
	public Office office;
}
