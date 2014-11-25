package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import models.base.BaseModel;

@Entity
@Table(name="contract_stamp_profiles")
public class ContractStampProfile extends BaseModel{
	
	private static final long serialVersionUID = 3503562995113282540L;

	@Column(name="fixed_working_time")
	public boolean fixedworkingtime;
	
	@Column(name="start_from")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate startFrom;
	
	@Column(name="end_to")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate endTo;
	
	@ManyToOne
	@JoinColumn(name="contract_id", nullable=false)
	public Contract contract;
	
}
