package models;

import com.google.common.collect.Range;
import java.util.Comparator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.ToString;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.Required;


/**
 * Contiene le informazioni per l'eventuale "reperibilità" svolta dalla persona.
 *
 * @author cristian
 */
@ToString
@Audited
@Entity
@Table(name = "person_reperibility")
public class PersonReperibility extends BaseModel {

  private static final long serialVersionUID = 7543768807724174894L;

  //@Unique
  @ManyToOne
  @Required
  @JoinColumn(name = "person_id")
  public Person person;

  @Column(name = "start_date")
  public LocalDate startDate;

  @Column(name = "end_date")
  public LocalDate endDate;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_reperibility_type_id")
  public PersonReperibilityType personReperibilityType;

  @OneToMany(mappedBy = "personReperibility", cascade = {CascadeType.REMOVE})
  public List<PersonReperibilityDay> personReperibilityDays;


  public String note;

  /**
   * il range di date di validità dell'appartenenza di una persona al servizio di reperibilità.
   * @return il range di date di validità dell'appartenenza di una persona al servizio di 
   *     reperibilità.
   */
  @Transient
  public Range<LocalDate> dateRange() {
    if (startDate == null && endDate == null) {
      return Range.all();
    }
    if (startDate == null) {
      return Range.atMost(endDate);
    }
    if (endDate == null) {
      return Range.atLeast(startDate);
    }
    return Range.closed(startDate, endDate);
  }

  public static Comparator<PersonReperibility> PersonReperibilityComparator 
      = new Comparator<PersonReperibility>() {

        public int compare(PersonReperibility pr1, PersonReperibility pr2) {

            String prName1 = pr1.personReperibilityType.description.toUpperCase();
            String prName2 = pr2.personReperibilityType.description.toUpperCase();


            return prName1.compareTo(prName2);
        }


      };
}
