package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.cache.Cache;
import play.data.validation.Email;
import play.db.jpa.Model;


@Audited
@Entity
@Table(name="conf_general")
public class ConfGeneral extends Model{

	/*inizio nuova configurazione della tabella*/
	@ManyToOne( fetch=FetchType.LAZY)
	@JoinColumn(name="office_id")
	public Office office;

	@Column(name="field")
	public String field;

	@Column(name="field_value")
	public String fieldValue;

	/*fine nuova configurazione della tabella*/

	//	@Column(name = "init_use_program")
	//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	//	public LocalDate initUseProgram;
	//	
	//	@Column(name = "institute_name")
	//	public String instituteName;
	//	
	//	@Column(name = "email_to_contact")
	//	@Email
	//	public String emailToContact;
	//	
	//	@Column(name = "seat_code")
	//	public Integer seatCode;
	//	
	//	@Column(name = "url_to_presence")
	//	public String urlToPresence;
	//	
	//	@Column(name="user_to_presence")
	//	public String userToPresence;
	//	
	//	@Column(name="password_to_presence")
	//	public String passwordToPresence;
	//	
	//	@Column(name="number_of_viewing_couple")
	//	public Integer numberOfViewingCoupleColumn;
	//	
	//	@Column(name="month_of_patron")
	//	public Integer monthOfPatron;
	//	
	//	@Column(name="day_of_patron")
	//	public Integer dayOfPatron;
	//	
	//	@Column(name="web_stamping_allowed")
	//	public boolean webStampingAllowed;
	//	
	//	//@OneToMany (mappedBy="confParameters", fetch = FetchType.LAZY)
	//	//spublic List<WebStampingAddress> webStampingAddressAllowed;
	//	
	//	@Column(name="meal_time_start_hour")
	//	public Integer mealTimeStartHour;
	//	
	//	@Column(name="meal_time_start_minute")
	//	public Integer mealTimeStartMinute;
	//	
	//	@Column(name="meal_time_end_hour")
	//	public Integer mealTimeEndHour;
	//	
	//	@Column(name="meal_time_end_minute")
	//	public Integer mealTimeEndMinute;





	public static ConfGeneral getConfGeneral()
	{
		ConfGeneral confGeneral = (ConfGeneral)Cache.get("confGeneral");
		if(confGeneral==null)
		{
			confGeneral = ConfGeneral.find("Select cg from ConfGeneral cg").first();
			Cache.set("confGeneral", confGeneral);
		}
		return confGeneral;
	}

	public static String getFieldValue(String field, Office office){
		String value = (String)Cache.get(field+office.name);
		if(value == null || value.equals("")){
			ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.field = ? and conf.office = ?", 
					field, office).first();
			value = conf.fieldValue;
			Cache.set(field+office.name, value);
		}
		return value;
	}

	public static ConfGeneral getConfGeneralByField(String field, Office office){
		
		ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.field = ? and conf.office = ?", 
				field, office).first();
				
		
		return conf;
	}

}
