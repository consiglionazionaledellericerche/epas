package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.db.jpa.Model;

@Entity
@Table(name="shift_cancelled")
public class ShiftCancelled extends Model{
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name="shift_type_id", nullable=false)
	public ShiftType type;
}
