package models;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.criteria.Fetch;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditQuery;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import com.google.common.collect.Multiset.Entry;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;
import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.db.jpa.Model;


/**
 * 
 * @author dario
 * 
 * Per adesso la classe Year recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista.
 */
@Entity
@Table(name = "year_recaps")
public class YearRecap extends Model{

	private static final long serialVersionUID = -5721503493068567394L;

	@Required
	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;

	@Required
	@Column
	public short year;
	@Column
	public int remaining;
	@Column
	public int remainingAp;
	@Column
	public int recg;
	@Column
	public int recgap;
	@Column
	public int overtime;
	@Column
	public int overtimeAp;
	@Column
	public int recguap;
	@Column
	public int recm;
	@Column
	public Timestamp lastModified;

	@Transient
	private List<String> months = null;
	@Transient
	private Map<AbsenceType,Integer> mappaAssenze = new HashMap<AbsenceType,Integer>();

	@Transient
	private LocalDate beginYear;

	protected YearRecap(){


	}
	

}
