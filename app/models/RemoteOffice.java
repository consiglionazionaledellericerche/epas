package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
 
@Entity
@Table(name="remote_office")
@DiscriminatorValue("R")
public class RemoteOffice extends Office {
 
    @Column(name="joining_date")
    private Date joiningDate;
     
    // Constructors and Getter/Setter methods, 
    
    @ManyToOne( fetch=FetchType.EAGER )
	@JoinColumn(name="office_id")
    public Office office;
}
