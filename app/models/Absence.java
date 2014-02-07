package models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.joda.time.LocalDate;

import play.data.Upload;
import play.data.validation.CheckWith;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.Blob;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends Model {
	
	private static final long serialVersionUID = -1963061850354314327L;
	
	@ManyToOne
	@JoinColumn(name = "absence_type_id")
	public AbsenceType absenceType;
	
	
	@ManyToOne(optional=false)
	@JoinColumn(name="personDay_id", nullable=false)
	public PersonDay personDay;
	
	@Column (name = "absence_file", nullable = true )
	public Blob absenceFile;

	@Override
	public String toString() {
		return String.format("Absence[%d] - personDay.id = %d, absenceType.id = %s", 
			id, personDay.id, absenceType.id);
	}
}
