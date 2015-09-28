package models;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Entity
@Audited
@Table(name="badges", uniqueConstraints={@UniqueConstraint(columnNames={"badge_reader_id", "code"})})
public class Badge extends BaseModel{
	
	@NotNull
	public String code;

	@ManyToOne
	public Person person;
	
	@ManyToOne
	@JoinColumn(name = "badge_reader_id")
	public BadgeReader badgeReader;

}
