package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.cache.Cache;
import play.data.validation.Email;
import play.db.jpa.Model;


@Audited
@Entity
@Table(name="conf_year")
public class ConfYear extends Model{
	
	@Column(name="year")
	public Integer year;
	
	@Column(name="month_expiry_vacation_past_year")
	public Integer monthExpiryVacationPastYear;

	@Column(name="day_expiry_vacation_past_year")
	public Integer dayExpiryVacationPastYear;
	
	@Column(name="month_expire_recovery_days_13")
	public Integer monthExpireRecoveryDaysOneThree;
	
	@Column(name="month_expire_recovery_days_49")
	public Integer monthExpireRecoveryDaysFourNine;

	@Column(name="max_recovery_days_13")
	public Integer maxRecoveryDaysOneThree;

	@Column(name="max_recovery_days_49")
	public Integer maxRecoveryDaysFourNine;	
	
	@Column(name="hour_max_to_calculate_worktime")
	public Integer hourMaxToCalculateWorkTime;
	
	public static ConfYear getConfYear()
	{
		ConfYear confYear = (ConfYear)Cache.get("confYear");
		if(confYear==null)
		{
			confYear = ConfYear.find("Select cy from ConfYear cy where cy.year = ?", new LocalDate().getYear()).first();
			if(confYear==null)
			{
				//TODO va creato per l'anno nuovo
			}
			Cache.set("confYear", confYear);
		}
		return confYear;
	}
	
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