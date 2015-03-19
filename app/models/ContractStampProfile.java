package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.joda.time.LocalDate;

@Entity
@Table(name="contract_stamp_profiles")
public class ContractStampProfile extends BaseModel{

	private static final long serialVersionUID = 3503562995113282540L;

	@Column(name="fixed_working_time")
	public boolean fixedworkingtime;

	@Column(name="start_from")

	public LocalDate startFrom;

	@Column(name="end_to")

	public LocalDate endTo;

	@ManyToOne
	@JoinColumn(name="contract_id", nullable=false)
	public Contract contract;

}
