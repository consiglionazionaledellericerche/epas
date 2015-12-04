package models.base;

import com.google.common.base.MoreObjects;

import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperModel;

import models.Person;

import play.db.jpa.GenericModel;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * Default base class per sovrascrivere la generazione delle nuove chiavi primarie.
 *
 * @author marco
 */
@MappedSuperclass
public abstract class BaseModel extends GenericModel {

  private static final long serialVersionUID = 4849404810311166199L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Transient
  public Long getId() {
    return id;
  }

  @Transient
  public String getLabel() {
    return this.toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).toString();
  }
  
  /**
   * Costruisce una istanza del wrapper se esiste.
   * @param wrapperFactory wrapperFactory
   * @return wrapper model 
   */
  @Transient
  public IWrapperModel getWrapper(IWrapperFactory wrapperFactory) {
    if (this instanceof Person) {
      return wrapperFactory.create((Person)this);
    }
    return null;
  }
}
