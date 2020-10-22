package models;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * Classe che modella un istituto.
 * 
 * @author alessandro
 */
@Audited
@Entity
@Table(name = "institutes")
public class Institute extends MutableModel {

  private static final long serialVersionUID = -2062459037430369402L;

  @Column(name = "perseo_id")
  public Long perseoId;
  
  @Unique
  @Required
  @NotNull
  public String name;

  /**
   * Codice univoco dell'istituto, per l'IIT è 044.
   */
  @Unique
  @NotNull
  public String cds;

  /**
   * sigla, ex.: IIT
   */
  @Unique
  public String code;

  @OneToMany(mappedBy = "institute")
  public Set<Office> seats = Sets.newHashSet();
  
  @Override
  public String getLabel() {
    return this.code;
  }
  
  @Override
  public String toString() {
    return getLabel();
  }

}
