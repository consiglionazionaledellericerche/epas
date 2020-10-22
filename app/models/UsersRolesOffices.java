package models;

import com.google.common.base.MoreObjects;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * IMPORTANTE: relazione con user impostata a LAZY per non scaricare tutte le informazioni della
 * persona durante la valutazione delle drools con target!=null. Avremmo potuto impostare a lazy la
 * successiva relazione fra user e person ma ciò non portava al risultato sperato (probabilmente a
 * causa della natura della relazione fra user e person OneToOne).
 */
@Entity
@Table(name = "users_roles_offices")
@Audited
public class UsersRolesOffices extends BaseModel {

  private static final long serialVersionUID = -1403683534643592790L;

  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @Required
  @NotNull
  public User user;

  
  @ManyToOne
  @JoinColumn(name = "office_id")
  @Required
  @NotNull
  @Unique("user office role")
  public Office office;

  
  @ManyToOne
  @JoinColumn(name = "role_id")
  @Required
  @NotNull
  public Role role;

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).omitNullValues()
        .add("id", id)
        .add("user", user)
        .add("role", role)
        .add("office", office)
        .toString();
  }

}
