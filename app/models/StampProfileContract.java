package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */

@Entity
@Table(name="stamp_profiles_contracts")
public class StampProfileContract extends BaseModel implements Comparable<StampProfileContract>{
	
	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="contract_id")
	public Contract contract;
	
	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="stamp_profile_id")
	public StampProfile stampProfile;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="start_from")
	public LocalDate startFrom;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_to")
	public LocalDate endTo;
	
	
	public int compareTo(StampProfileContract compareSPC)
	{
		if (startFrom.isBefore(compareSPC.startFrom))
			return -1;
		else if (startFrom.isAfter(compareSPC.startFrom))
			return 1;
		else
			return 0; 
	}
	
}
