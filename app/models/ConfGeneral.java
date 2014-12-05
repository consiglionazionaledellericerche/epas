package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.cache.Cache;



@Audited
@Entity
@Table(name="conf_general")
public class ConfGeneral extends BaseModel{
	
/*	
	  1 | init_use_program         | 2012-07-30                                       |         1
	  2 | institute_name           | IIT                                              |         1
	  3 | email_to_contact         |                                                  |         1
	  4 | seat_code                | 223400                                           |         1
	  5 | url_to_presence          | http://attestati-cnr.devel.iit.cnr.it/attestati/ |         1
	  6 | user_to_presence         |                                                  |         1
	  7 | password_to_presence     |                                                  |         1
	  8 | number_of_viewing_couple | 3                                                |         1
	  9 | month_of_patron          | 6                                                |         1
	 10 | day_of_patron            | 17                                               |         1
	 11 | web_stamping_allowed     | false                                            |         1
	 12 | meal_time_start_hour     | 1                                                |         1
	 13 | meal_time_start_minute   | 0                                                |         1
	 14 | meal_time_end_hour       | 23                                               |         1
	 15 | meal_time_end_minute     | 0                                                |         1
*/
	
	private static final long serialVersionUID = 4941937973447699263L;
	
	public final static String INIT_USE_PROGRAM = "init_use_program";
	public final static String MONTH_OF_PATRON = "month_of_patron";
	public final static String DAY_OF_PATRON = "day_of_patron";
	public final static String WEB_STAMPING_ALLOWED = "web_stamping_allowed";
	public final static String NUMBER_OF_VIEWING_COUPLE = "number_of_viewing_couple";
	public final static String USER_TO_PRESENCE = "user_to_presence";
	public final static String PASSWORD_TO_PRESENCE = "password_to_presence";
	public final static String URL_TO_PRESENCE = "url_to_presence";
	public final static String EMAIL_TO_CONTACT = "email_to_contact";
	public final static String DATE_START_MEAL_TICKET = "date_start_meal_ticket";
	
	
	public final static String MEAL_TIME_START_HOUR = "meal_time_start_hour";
	public final static String MEAL_TIME_START_MINUTE = "meal_time_start_minute";
	public final static String MEAL_TIME_END_HOUR = "meal_time_end_hour";
	public final static String MEAL_TIME_END_MINUTE = "meal_time_end_minute";
	

	/*inizio nuova configurazione della tabella*/
	@ManyToOne( fetch=FetchType.LAZY)
	@JoinColumn(name="office_id")
	public Office office;

	@Column(name="field")
	public String field;

	@Column(name="field_value")
	public String fieldValue;

	
	public ConfGeneral() {
		this.office = null;
		this.field = null;
		this.fieldValue = null;
	}
	
	public ConfGeneral(Office office, String fieldName, String fieldValue) {
		this.office = office;
		this.field = fieldName;
		this.fieldValue = fieldValue;
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
	
	/**
	 * Produce la configurazione generale di default al momento della creazione di una nuova sede
	 * @param office
	 */
	public static void buildDefaultConfGeneral(Office office) {

		//TODO inserire i dati di default in un file di configurazione
		
		ConfGeneral confGeneral;

		LocalDate beginYear = new LocalDate(LocalDate.now().getYear(), 1, 1);
		confGeneral = new ConfGeneral(office, ConfGeneral.INIT_USE_PROGRAM, beginYear.toString());
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.DATE_START_MEAL_TICKET,null);
		confGeneral.save();

		confGeneral = new ConfGeneral(office, ConfGeneral.DAY_OF_PATRON, "1");
		confGeneral.save();

		confGeneral = new ConfGeneral(office, ConfGeneral.MONTH_OF_PATRON, "1");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.USER_TO_PRESENCE, null);
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.PASSWORD_TO_PRESENCE, null);
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.URL_TO_PRESENCE, "https://attestati.rm.cnr.it/attestati/" );
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.NUMBER_OF_VIEWING_COUPLE, "2");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.WEB_STAMPING_ALLOWED, "false");
		confGeneral.save();
		
		
		//TODO SPORTARLI NELLA CONFIGURAZIONE PERIODICA QUANDO CI SARA'
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_START_HOUR, "1");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_START_MINUTE, "0");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_END_HOUR, "23");
		confGeneral.save();
		
		confGeneral = new ConfGeneral(office, ConfGeneral.MEAL_TIME_END_MINUTE, "0");
		confGeneral.save();
	}
	
	
}
