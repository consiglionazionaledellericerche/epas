package models;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Unique;

@Audited
@Entity
@Table(name="badges",uniqueConstraints={@UniqueConstraint(columnNames={"person_id", "number"})})
public class Badge extends BaseModel{

	private static final long serialVersionUID = 6883290376912865772L;

	@Unique(value="person number")
	@Required
	@NotNull
	@Column(nullable=false)
	public String number;

	@NotNull
	@ManyToOne(optional=false)
	@JoinColumn(nullable=false)
	public Person person;

	/**
	 * Data inizio validità del badge
	 */
	@Column(name = "start_date")
	public LocalDate startDate;

	/**
	 * Data fine validità del badge
	 */
	@Column(name = "end_date")
	public LocalDate endDate;
}