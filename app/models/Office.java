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

import com.google.common.collect.Lists;

 
 
@Entity
@Audited
@Table(name = "office")
public class Office extends BaseModel{
 
	private static final long serialVersionUID = -8689432709728656660L;

	@Required
	@Column(name = "name")
    public String name;
    
    @Column(name = "contraction")
    public String contraction;
    
    @Column(name = "address")
    public String address = "";
    
    @Column(name = "code")
    public Integer code;
    
    @Column(name="joining_date")
    public LocalDate joiningDate;
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Office> subOffices = new ArrayList<Office>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="office_id")
    public Office office;
    
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
    
	@Override
	public String toString() {
		return getLabel();
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
	
}
