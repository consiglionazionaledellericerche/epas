package models.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperModel;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;
import models.Person;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.envers.NotAudited;
import org.jadira.usertype.dateandtime.joda.PersistentYearMonthAsString;
import org.joda.time.YearMonth;
import play.db.jpa.GenericModel;

/**
 * Default base class per sovrascrivere la generazione delle nuove chiavi primarie.
 *
 * @author marco
 */
@TypeDefs(@TypeDef(name = "YearMonth", defaultForType = YearMonth.class,
    typeClass = PersistentYearMonthAsString.class))
@MappedSuperclass
public abstract class BaseModel extends GenericModel {

  private static final long serialVersionUID = 4849404810311166199L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @JsonIgnore
  @NotAudited
  @Version
  public Integer version;

  @Transient
  public Long getId() {
    return id;
  }

  @Transient
  public String getLabel() {
    return toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).toString();
  }

  /**
   * Costruisce una istanza del wrapper se esiste.
   *
   * @param wrapperFactory wrapperFactory
   * @return wrapper model
   */
  @Transient
  public IWrapperModel<?> getWrapper(IWrapperFactory wrapperFactory) {
    if (this instanceof Person) {
      return wrapperFactory.create((Person) this);
    }
    return null;
  }
}
