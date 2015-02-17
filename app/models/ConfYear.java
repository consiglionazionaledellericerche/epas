package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;



@Audited
@Entity
@Table(name="conf_year")
public class ConfYear extends BaseModel{
	
	/*
	  1 | month_expiry_vacation_past_year | 8           | 2014 |         1
	  2 | day_expiry_vacation_past_year   | 31          | 2014 |         1
	  3 | month_expire_recovery_days_13   | 0           | 2014 |         1
	  4 | month_expire_recovery_days_49   | 4           | 2014 |         1
	  5 | max_recovery_days_13            | 22          | 2014 |         1
	  6 | max_recovery_days_49            | 0           | 2014 |         1
	  7 | hour_max_to_calculate_worktime  | 5           | 2014 |         1
	*/
	
	private static final long serialVersionUID = -3157754270960969163L;
	
	public final static String MONTH_EXPIRY_VACATION_PAST_YEAR = "month_expiry_vacation_past_year";
	public final static String DAY_EXPIRY_VACATION_PAST_YEAR = "day_expiry_vacation_past_year";
	public final static String MONTH_EXPIRY_RECOVERY_DAYS_13 = "month_expire_recovery_days_13";
	public final static String MONTH_EXPIRY_RECOVERY_DAYS_49 = "month_expire_recovery_days_49";
	public final static String MAX_RECOVERY_DAYS_13 = "max_recovery_days_13";
	public final static String MAX_RECOVERY_DAYS_49 = "max_recovery_days_49";
	public final static String HOUR_MAX_TO_CALCULATE_WORKTIME = "hour_max_to_calculate_worktime";
	
	
	
	
	/* nuova configurazione della tabella*/
	@ManyToOne( fetch=FetchType.LAZY)
	@JoinColumn(name="office_id")
	public Office office;
	
	@Column(name="year")
	public Integer year;
	
	@Column(name="field")
	public String field;
	
	@Column(name="field_value")
	public String fieldValue;
	
	public ConfYear() {
		this.year = null;
		this.office = null;
		this.field = null;
		this.fieldValue = null;
	}
	
	public ConfYear(Office office, Integer year, String fieldName, String fieldValue) {
		this.office = office;
		this.year = year;
		this.field = fieldName;
		this.fieldValue = fieldValue;
	}
	


	public String getIntelligibleMonthValue(Integer i)
	{
		if(i==0)
			return "nessun limite";
		if(i==1)
			return "entro gennaio";
		if(i==2)
			return "entro febbraio";
		if(i==3)
			return "entro marzo";
		if(i==4)
			return "entro aprile";
		if(i==5)
			return "entro maggio";
		if(i==6)
			return "entro giugno";
		if(i==7)
			return "entro luglio";
		if(i==8)
			return "entro agosto";
		if(i==9)
			return "entro settembre";
		if(i==10)
			return "entro ottobre";
		if(i==11)
			return "entro novembre";
		if(i==12)
			return "entro dicembre";
		return null;
		
	}
	
	public String getIntelligibleNumberValue(Integer i)
	{
		if(i==0)
			return "nessun limite";
		else
			return i+"";
	}

}
