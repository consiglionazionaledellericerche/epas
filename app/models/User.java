package models;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import models.base.BaseModel;
import models.enumerate.AccountRole;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Unique;

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

  @MinSize(5)
  public String password;

  @NotAudited
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  public Person person;

  @NotAudited
  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  public List<BadgeReader> badgeReaders = Lists.newArrayList();

  @ElementCollection
  @Enumerated(EnumType.STRING)
  public Set<AccountRole> roles = Sets.newHashSet();

  
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

  @Nullable
  @ManyToOne
  @JoinColumn(name = "office_owner_id")
  public Office owner;

  /**
   * Ritorna il badgeReader associato all'utente se ne ha almeno uno associato.
   * @return il badgeReader associato all'utente se ne ha almeno uno associato.
   */
  @Transient
  public BadgeReader getBadgeReader() {
    if (badgeReaders.size() > 0) {
      return badgeReaders.get(0);
    }
    return null;
  }
  
  @Override
  public String getLabel() {
    if (this.person != null) {
      return this.person.fullName() + " - " + this.person.office.name;
    } else if (this.getBadgeReader() != null) {
      return this.getBadgeReader().code;

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
   * True se l'user ha un ruolo di sistema.
   */
  public boolean isSystemUser() {
    return !roles.isEmpty();
  }

  /**
   * L'office fornito ha un legame con questo uro.
   *
   * @return true se ce l'ha, false altrimenti.
   */
  public boolean hasRelationWith(Office office) {
    return owner == office || (person != null && person.office == office);
  }

  /**
   * True se l'utente ha almeno uno dei ruoli passati tra i parametri,
   * false altrimenti.
   * @param args Stringhe corrispondenti ai ruoli da verificare.
   * @return true se contiene almeno uno dei ruoli specificati.
   */
  public boolean hasRoles(String... args) {
    return usersRolesOffices.stream()
        .anyMatch(uro -> Arrays.asList(args).contains(uro.role.name));
  }
}
