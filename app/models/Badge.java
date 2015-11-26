package models;

import models.base.BaseModel;
import play.data.binding.As;
import play.data.validation.Required;

import org.hibernate.envers.Audited;

import it.cnr.iit.epas.NullStringBinder;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Audited
@Table(name="badges", uniqueConstraints={@UniqueConstraint(columnNames={"badge_reader_id", "code"})})
public class Badge extends BaseModel{
	
    @Required
    @As(binder=NullStringBinder.class)
	@NotNull
	public String code;

	@ManyToOne
	public Person person;
	
	@ManyToOne
	@JoinColumn(name = "badge_reader_id")
	public BadgeReader badgeReader;

}
