package models;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import net.sf.oval.constraint.MinLength;
import play.data.validation.Required;
import play.db.jpa.Model;

@Audited
@Entity
@Table(name="stamp_modification_types")
public class StampModificationType extends Model{
	
	@Required
	public String code;
	
	@Required
	@MinLength(value=2)
	public String description;	

	
	@OneToMany(mappedBy="stampModificationType")
	public Set<Stamping> stampings; 

}
