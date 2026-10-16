/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import common.security.PasswordHasher;
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
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.enumerate.AccountRole;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.Play;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Unique;
import play.libs.Codec;

/**
 * Un utente di ePAS.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
public class User extends BaseModel {

  private static final long serialVersionUID = -6039180733038072891L;

  /**
   * Parametro di configurazione per mantenere l'hash MD5 delle password.
   */
  public static final String KEEP_MD5_PASSWORD = "security.password.keepMd5";

  @Unique
  @NotNull
  @Column(nullable = false)
  @Required
  private String username;

  /**
   * Hash MD5 non salato della password: resta per le password non ancora aggiornate a
   * {@link #passwordHash} ed è valorizzato solo se {@link #KEEP_MD5_PASSWORD} è attivo.
   * Viene usato anche per ricevere la nuova password in chiaro dai form prima di
   * {@link #updatePassword(String)}.
   */
  @Deprecated
  @MinSize(5)
  private String password;

  /**
   * Hash SHA-512 non salato della password, non più valorizzato: resta per le password non
   * ancora aggiornate a {@link #passwordHash}.
   */
  @Deprecated
  private String passwordSha512;

  /**
   * Hash della password calcolato con {@link PasswordHasher} (non salvato nello storico).
   */
  @NotAudited
  private String passwordHash;
  
  /**
   * Corrisponde ad un identificato univoco nella propria 
   * organizzazione per questo utente.
   * Se l'utente è una persona deve corrispondere con il campo
   * eppn.
   */
  private String subjectId;
  
  @NotAudited
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  private Person person;

  @NotAudited
  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<BadgeReader> badgeReaders = Lists.newArrayList();

  @ElementCollection
  @Enumerated(EnumType.STRING)
  private Set<AccountRole> roles = Sets.newHashSet();

  
  @OneToMany(mappedBy = "user", cascade = {CascadeType.REMOVE})
  private List<UsersRolesOffices> usersRolesOffices = new ArrayList<UsersRolesOffices>();

  @Column(name = "expire_recovery_token")
  private LocalDate expireRecoveryToken;

  @Column(name = "recovery_token")
  private String recoveryToken;

  @Column(name = "disabled")
  private boolean disabled;

  @Column(name = "expire_date")
  private LocalDate expireDate;

  @Nullable
  @ManyToOne
  @JoinColumn(name = "office_owner_id")
  private Office owner;

  private String keycloakId;

  /**
   * Ritorna il badgeReader associato all'utente se ne ha almeno uno associato.
   *
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
      return this.person.fullName() + " - " + this.person.getOffice().getName();
    } else if (this.getBadgeReader() != null) {
      return this.getBadgeReader().getCode();

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
    return owner == office || (person != null && person.getOffice() == office);
  }

  /**
   * True se l'utente ha almeno uno dei ruoli passati tra i parametri,
   * false altrimenti.
   *
   * @param args Stringhe corrispondenti ai ruoli da verificare.
   * @return true se contiene almeno uno dei ruoli specificati.
   */
  public boolean hasRoles(String... args) {
    return usersRolesOffices.stream()
        .anyMatch(uro -> Arrays.asList(args).contains(uro.getRole().getName()));
  }

  public boolean hasAccountRoles(AccountRole... args) {
    return roles.stream()
        .anyMatch(role -> Arrays.asList(args).contains(role));
  }

  /**
   * Imposta la nuova password salvandone l'hash con salt, ed elimina gli hash non salati.
   * L'hash MD5 viene mantenuto aggiornato se è attivo {@link #KEEP_MD5_PASSWORD}.
   *
   * @param newPassword la nuova password in chiaro
   */
  public void updatePassword(String newPassword) {
    passwordHash = PasswordHasher.hash(newPassword);
    password = keepMd5Password() ? cryptPasswordMd5(newPassword) : null;
    passwordSha512 = null;
  }

  /**
   * Indica se mantenere anche l'hash MD5 delle password, per le istanze che lo forniscono
   * ad altri sistemi (es. per popolare un LDAP). Non attivo per default.
   *
   * @return true se l'hash MD5 delle password va mantenuto, false altrimenti.
   */
  public static boolean keepMd5Password() {
    return "true".equalsIgnoreCase(Play.configuration.getProperty(KEEP_MD5_PASSWORD, "false"));
  }

  /**
   * Verifica la password, anche rispetto agli hash non salati delle password non ancora
   * aggiornate.
   *
   * @param plain la password in chiaro
   * @return true se la password corrisponde, false altrimenti.
   */
  public boolean checkPassword(String plain) {
    if (Strings.isNullOrEmpty(plain)) {
      return false;
    }
    if (passwordHash != null) {
      return PasswordHasher.matches(plain, passwordHash);
    }
    return (password != null && password.equals(cryptPasswordMd5(plain)))
        || (passwordSha512 != null && passwordSha512.equals(cryptPasswordSha512(plain)));
  }

  /**
   * Indica se l'hash della password va ricalcolato: password salvata solo con hash non salati,
   * con parametri non più attuali, oppure senza l'hash MD5 quando è richiesto di mantenerlo.
   *
   * @return true se dopo una verifica riuscita la password va salvata di nuovo.
   */
  public boolean passwordNeedsRehash() {
    return passwordHash == null || PasswordHasher.needsRehash(passwordHash)
        || (keepMd5Password() && password == null);
  }

  public static String cryptPasswordMd5(String newPassword) {
    return Codec.hexMD5(newPassword);
  }

  public static String cryptPasswordSha512(String newPassword) {
    return Hashing.sha512().hashString(newPassword, Charsets.UTF_8).toString();
  }

  public boolean passwordSha512Match(String plain) {
    return Hashing.sha512().hashString(plain, Charsets.UTF_8).toString().equals(password);
  }

}
