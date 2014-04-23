package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.cache.Cache;
import play.db.jpa.Model;


@Audited
@Entity
@Table(name="conf_year")
public class ConfYear extends Model{
	
	/* nuova configurazione della tabella*/
	@ManyToOne( fetch=FetchType.LAZY)
	@JoinColumn(name="office_id")
	public Office office;
	
	@Column(name="year")
	public Integer year;
	
	@Column(name="field")
	public String field;
	
	@Column(name="field_value")
	public Integer fieldValue;
	
	/*fine configurazione della tabella*/
	
	
	
//	@Column(name="month_expiry_vacation_past_year")
//	public Integer monthExpiryVacationPastYear;
//
//	@Column(name="day_expiry_vacation_past_year")
//	public Integer dayExpiryVacationPastYear;
//	
//	@Column(name="month_expire_recovery_days_13")
//	public Integer monthExpireRecoveryDaysOneThree;
//	
//	@Column(name="month_expire_recovery_days_49")
//	public Integer monthExpireRecoveryDaysFourNine;
//
//	@Column(name="max_recovery_days_13")
//	public Integer maxRecoveryDaysOneThree;
//
//	@Column(name="max_recovery_days_49")
//	public Integer maxRecoveryDaysFourNine;	
//	
//	@Column(name="hour_max_to_calculate_worktime")
//	public Integer hourMaxToCalculateWorkTime;
	
	
	
	public static ConfYear getConfYear(Integer year)
	{
		ConfYear confYear = (ConfYear)Cache.get("confYear"+year);
		if(confYear==null)
		{
			confYear = ConfYear.find("Select cy from ConfYear cy where cy.year = ?", year).first();
			if(confYear==null)
			{
				//TODO va creato per l'anno richiesto ma inesistente??
			}
			Cache.set("confYear"+year, confYear);
		}
		return confYear;
	}

	public static Integer getFieldValue(String field, Integer year, Office office) {
		Integer value = (Integer)Cache.get(field+year);
		if(value == null){
			ConfYear conf = ConfYear.find("Select cy from ConfYear cy where cy.year = ? and cy.field = ? and cy.office = ?", 
					year, field, office).first();
			value = conf.fieldValue;
			Cache.set(field+year, value);
		}
		return value;
	}
	
	
	public static ConfYear getConfGeneralByFieldAndYear(String field, Integer year, Office office){
		
		ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.office = ? and conf.year = ?", 
				field, office, year).first();
				
		
		return conf;
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