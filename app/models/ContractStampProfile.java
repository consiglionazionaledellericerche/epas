package models;

import com.google.common.collect.Range;
import models.base.BaseModel;
import org.joda.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

	public boolean includeDate(LocalDate date){
		if(startFrom== null && endTo==null){
//			TODO decidere se considerare l'intervallo infinito, oppure nullo
			return false;
		}
		if(startFrom == null){
			return !endTo.isAfter(date);
		}
		if(endTo==null){
			return !startFrom.isBefore(date);
		}	
		return !startFrom.isBefore(date) && !endTo.isAfter(date);
	}
	
	public Range<LocalDate> dateRange(){
		if(startFrom== null && endTo==null){
			return Range.all();
		}
		if(startFrom == null){
			return Range.atMost(endTo);
		}
		if(endTo==null){
			return Range.atLeast(startFrom);
		}	
		return Range.closed(startFrom, endTo);
	}

}
