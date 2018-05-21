package models.absences;

import com.beust.jcommander.internal.Lists;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.data.validation.URL;
import play.db.jpa.Blob;

/**
 * Allegato o indirizzo web di documento amministrativo.
 * 
 * @author cristian
 * @author dario
 */
@Audited
@Entity
@Table(name = "contractual_references")
public class ContractualReference extends BaseModel {
  
  private static final long serialVersionUID = 53012052329220325L;

  @NotNull
  @Required
  public String name;

  @URL
  public String url;
  
  public String filename;

  public Blob file;

  @ManyToMany(mappedBy = "contractualReferences")
  List<ContractualClause> contractualClauses = Lists.newArrayList();
  
  @Transient
  public long getLength() {
    return file == null ? 0 : file.length();
  }

  @PreRemove
  private void onDelete() {
    this.file.getFile().delete();
  }
}
