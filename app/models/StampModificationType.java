package models;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import com.google.common.base.Optional;

import dao.StampingDao;
import models.base.BaseModel;
import net.sf.oval.constraint.MinLength;
import play.data.validation.Required;


@Audited
@Entity
@Table(name="stamp_modification_types")
public class StampModificationType extends BaseModel{
	
	private static final long serialVersionUID = 8403725731267832733L;

	@Required
	public String code;
	
	@Required
	@MinLength(value=2)
	public String description;	

	
	@OneToMany(mappedBy="stampModificationType")
	public Set<Stamping> stampings;
	
	@OneToMany(mappedBy="stampModificationType")
	public List<PersonDay> personDays;

}
