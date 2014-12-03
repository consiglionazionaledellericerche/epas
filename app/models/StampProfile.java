/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;


/**
 * @author cristian
 *
 */
@Entity
@Table(name = "stamp_profiles")
@Deprecated
public class StampProfile extends BaseModel {

	private static final long serialVersionUID = 5187385003376986175L;

	@Required
	@ManyToOne
	@JoinColumn(name="person_id", nullable=false)
	public Person person;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="start_from")
	public LocalDate startFrom;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_to")
	public LocalDate endTo;


	/**
	 * Corrisponde alla voce "Presenza predefinita" della vecchia applicazione
	 * Quando è true viene impostato nel PersonDay l'orario di lavoro previsto
	 * per il giorno indipendentemente dalla presenza di timbrature.
	 * Nel caso ci siano timbrature viene impostato nel PersonDay il minimo tra
	 * l'orario di lavoro previsto per il giorno ed il timeAtWork calcolato con 
	 * le timbrature presenti
	 */
	public boolean fixedWorkingTime;

	//TODO eliminare e configurare yaml
//	public void setStartFrom(String date){
//		this.startFrom = new LocalDate(date);
//	}
//	//TODO eliminare e configurare yaml
//	public void setEndTo(String date){
//		this.endTo = new LocalDate(date);
//	}
	

	
	
	/**
	 * 
	 * @param date
	 * @return lo stamp profile attivo alla data passata come parametro
	 */
	public static StampProfile getCurrentStampProfile(Person person, LocalDate date){
		Contract c = person.getContract(date);
		if (c == null)
			return null;
		StampProfile sp = StampProfile.find("Select sp from StampProfile sp where sp.contract.person = ? and "
				+ "(sp.stampProfileContract.startFrom < ? and sp.stampProfileContract.endTo > ? " +
				"or sp.stampProfileContract.startFrom < ? and sp.stampProfileContract.endTo is null) "
				+ "order by sp.stampProfileContract.startFrom desc", person, date, date, date).first(); 
		if(sp == null){
			StampProfile spOld = StampProfile.find("Select sp from StampProfile sp where sp.person = ? order by sp.startFrom desc", person).first();
			StampProfile nuovoStampProfile = new StampProfile();
			nuovoStampProfile.person = person;
			nuovoStampProfile.startFrom = spOld.endTo.plusDays(1);
			nuovoStampProfile.endTo = c.expireContract;
			nuovoStampProfile.fixedWorkingTime = false;
//			nuovoStampProfile.description = "";
			nuovoStampProfile.save();
			return nuovoStampProfile;
		}
		return sp;
	}
	
	

}
