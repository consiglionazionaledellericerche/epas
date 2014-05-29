package models;

import javax.persistence.Entity;
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

	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	public Integer numberOfHours;
	
	public Integer year;
}
