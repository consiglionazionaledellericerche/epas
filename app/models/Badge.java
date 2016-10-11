package models;

import it.cnr.iit.epas.NullStringBinder;

import lombok.EqualsAndHashCode;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.binding.As;
import play.data.validation.Required;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * Sono stati implementati i metodi Equals e HashCode in modo che Se sono presenti più badge per la
 * persona che differiscono solo per il campo badgeReader venga restituito un solo elemento
 * (effettivamente per noi è lo stesso badge) Quindi person.badges non restituisce i duplicati.
 */
@Entity
@Audited
@EqualsAndHashCode(exclude = {"badgeReader"})
@Table(name = "badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"badge_reader_id", "code"})})
public class Badge extends BaseModel {

  private static final long serialVersionUID = -4397151725225276985L;

  @Required
  @As(binder = NullStringBinder.class)
  @NotNull
  public String code;

  @ManyToOne
  public Person person;

  @ManyToOne
  @JoinColumn(name = "badge_reader_id")
  public BadgeReader badgeReader;

  @ManyToOne
  @JoinColumn(name = "badge_system_id")
  public BadgeSystem badgeSystem;
}
