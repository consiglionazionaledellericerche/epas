package models;

import com.google.common.base.MoreObjects;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Audited
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
public class User extends BaseModel {

  private static final long serialVersionUID = -6039180733038072891L;

  @Unique
  @NotNull
  @Column(nullable = false)
  @Required
  public String username;

  @Required
  public String password;

  @NotAudited
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  public Person person;

  @NotAudited
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  public BadgeReader badgeReader;

  @NotAudited
  @OneToMany(mappedBy = "user", cascade = {CascadeType.REMOVE})
  public List<UsersRolesOffices> usersRolesOffices = new ArrayList<UsersRolesOffices>();


  @Column(name = "expire_recovery_token")
  public LocalDate expireRecoveryToken;

  @Column(name = "recovery_token")
  public String recoveryToken;
  
  @Column(name = "disabled")
  public boolean disabled;
  
  @Column(name = "expire_date")
  public LocalDate expireDate;

  @Override
  public String getLabel() {
    if (this.person != null) {
      return this.person.fullName() + " - " + this.person.office.name;
    } else if (this.badgeReader != null) {
      return this.badgeReader.code;

    } else {
      return this.username;
    }

  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", this.id)
            .add("user", this.username)
            .toString();
  }

  /**
   * Se l'user è un account di sistema. TODO: definire la logica più dettagliata se necessario.
   */
  public boolean isSystemUser() {
    if (person == null) {
      return true;
    }
    return false;
  }

  /**
   * Se l'user il super amministratore TODO: definire la logica più dettagliata se necessario.
   */
  public boolean isSuperAdmin() {
    return username.equals("admin") || username.equals("developer");
  }

}
