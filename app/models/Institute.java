package models;

import com.google.common.collect.Sets;

import models.base.MutableModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author alessandro
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
   * Codice univoco dell'istituto, per l'IIT è 044
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
}
