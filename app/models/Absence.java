package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.db.jpa.Blob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * @author dario
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends BaseModel {

  private static final long serialVersionUID = -1963061850354314327L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_type_id")
  public AbsenceType absenceType;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "personDay_id", nullable = false)
  public PersonDay personDay;

  @Column(name = "justified_minutes", nullable = true)
  public Integer justifiedMinutes;

  @Column(name = "absence_file", nullable = true)
  public Blob absenceFile;
  /**
   * Questo campo serve nel caso in cui debba essere valorizzata la data di inserimento di
   * un'assenza senza che questa debba essere associata a un personDay. In particolare nel caso in
   * cui venga richiesto via rest il controllo sulla possibilità di inserire un certo codice di
   * assenza, senza che questo debba essere persistito, questo campo viene valorizzato per tenere
   * traccia di quale giorno debba contenere l'assenza in modo che, successivamente, possano essere
   * eseguiti tutti i calcoli senza però impattare sul database
   */
  @Transient
  public LocalDate date;

  @Override
  public String toString() {
    return String.format("Absence[%d] - personDay.id = %d, absenceType.id = %s",
            id, personDay.id, absenceType.id);
  }
}
