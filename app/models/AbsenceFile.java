package models;

import play.*;
import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.*;

import javax.persistence.*;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import java.util.*;

@Entity
@Table(name = "absence_files")
public class AbsenceFile extends Model {

		public Blob file;
	    public String fileName;
	    
	    @Required
		@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	    public LocalDate uploadDate;
	    
	    @Required
		@OneToOne(mappedBy = "absenceFile", optional = false)
		@JoinColumn(name = "absenceFile_id", nullable = false)
	    public Absence absence;
    
}
