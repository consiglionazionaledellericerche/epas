package models;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import play.data.validation.Required;
import play.data.validation.Unique;
import models.base.BaseModel;
import models.base.MutableModel;

/**
 * @author alessandro
 *
 */
@Audited
@Entity
@Table(name = "institutes")
public class Institute extends MutableModel {

	@Unique
	@Required
	@NotNull
	public String name;

	/**
	 * sigla, ex.: IIT
	 */
	@Unique
	public String code;
	
	@OneToMany(mappedBy="institute")
	public Set<Office> seats = Sets.newHashSet();

}
