package models.base;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.User;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.joda.time.LocalDateTime;

/**
 * Definisce il mapping con le revisioni envers.
 * @author marco
 */
@Entity
@RevisionEntity(ExtendedRevisionListener.class)
@Table(name = "revinfo")
public class Revision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  @Column(name = "rev")
  public int id;

  @RevisionTimestamp
  @Column(name = "revtstmp")
  public long timestamp;
  @ManyToOne(optional = true)
  public User owner;
  // ip address
  public String ipaddress;

  @Transient
  public LocalDateTime getRevisionDate() {
    return new LocalDateTime(timestamp);
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof Revision) {
      final Revision other = (Revision) obj;
      return id == other.id;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("date", getRevisionDate())
            .add("owner", owner)
            .add("ipaddress", ipaddress)
            .omitNullValues()
            .toString();
  }
}
