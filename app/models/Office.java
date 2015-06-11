package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Unique;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
 
@Entity
@Audited
@Table(name = "office")
public class Office extends BaseModel{
 
	private static final long serialVersionUID = -8689432709728656660L;

	@Required
	@Column(name = "name")
    public String name;
    
	@Unique
    @Column(name = "contraction")
    public String contraction;
    
    @Column(name = "address")
    public String address = "";
    
	//sedeId, serve per l'invio degli attestati, per esempio per la sede di Pisa è "223400"
    @Column(name = "codeId")
    public String codeId;
    
    @Column(name="joining_date")
    public LocalDate joiningDate;
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Office> subOffices = new ArrayList<Office>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="office_id")
    public Office office;
    
	/**
	 * Centro Di Spesa , per l'istituto di informatica è 044
	 */
    @Unique
    public String cds;
    
	//Codice della sede, per esempio per la sede di Pisa è "044000"
	@Unique
	public String code;
    
    //@OneToMany(mappedBy="restOwner", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    //public List<User> restUsers = new ArrayList<User>();
     
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Person> persons = new ArrayList<Person>();
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<ConfGeneral> confGeneral = new ArrayList<ConfGeneral>();
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<ConfYear> confYear = new ArrayList<ConfYear>();
    
    @NotAudited
    @OneToMany(mappedBy="office", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
    
    @NotAudited
	@OneToMany(mappedBy="office", fetch=FetchType.LAZY)
	public List<WorkingTimeType> workingTimeType = new ArrayList<WorkingTimeType>();
    
    @NotAudited
	@OneToMany(mappedBy="office", fetch=FetchType.LAZY)
	public List<TotalOvertime> totalOvertimes = new ArrayList<TotalOvertime>();
    
    
    @Transient
    private Boolean isEditable = null;
    
    public String getName() {
    	return this.name;
    }
    
    @Override
    public String getLabel() {
    	return this.name;
    }

	@Transient
	public List<WorkingTimeType> getEnabledWorkingTimeType() {
		
		List<WorkingTimeType> enabledWttList = new ArrayList<WorkingTimeType>();
		for(WorkingTimeType wtt: this.workingTimeType) {
			
			if(wtt.disabled == false)
				enabledWttList.add(wtt);
		}
		return enabledWttList;
	}
	
	@Transient
	public void copy(Office office){
		name = Strings.isNullOrEmpty(office.name) ? name : office.name;
		contraction = Strings.isNullOrEmpty(office.contraction) ? contraction : office.contraction;
		address = Strings.isNullOrEmpty(office.address) ? address : office.address;
		codeId = Strings.isNullOrEmpty(office.codeId) ? codeId : office.codeId;
		joiningDate = office.joiningDate == null ? joiningDate : office.joiningDate;
		cds = Strings.isNullOrEmpty(office.cds) ? cds : office.cds;
		code = Strings.isNullOrEmpty(office.code) ? code : office.code;
	}
	
}
