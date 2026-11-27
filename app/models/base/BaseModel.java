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
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.jadira.usertype.dateandtime.joda.PersistentYearMonthAsString;
import org.joda.time.YearMonth;
import play.db.jpa.GenericModel;

/**
 * Default base class per sovrascrivere la generazione delle nuove chiavi primarie.
 *
 * @author Marco Andreini
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
   * Due entità sono uguali se sono la stessa istanza oppure se hanno lo stesso id e la stessa
   * classe (i proxy Hibernate sono confrontati con la classe dell'entità, senza inizializzarli).
   *
   * <p>Sostituisce JPABase.equals, che a ogni chiamata cerca per reflection il campo chiave:
   * il confronto è usato di continuo dalle regole Drools dei permessi (es. "contains" sui ruoli
   * della sede), eseguite mentre è preso il lock della rulebase condiviso da tutte le richieste.
   * Come in JPABase, un'entità senza id è uguale solo a sé stessa.</p>
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BaseModel)) {
      return false;
    }
    final Long key = identifier(this);
    if (key == null) {
      return false;
    }
    final Class<?> thisClass = HibernateProxyHelper.getClassWithoutInitializingProxy(this);
    final Class<?> otherClass = HibernateProxyHelper.getClassWithoutInitializingProxy(other);
    if (!thisClass.isAssignableFrom(otherClass) && !otherClass.isAssignableFrom(thisClass)) {
      return false;
    }
    return key.equals(identifier((BaseModel) other));
  }

  /**
   * L'hashCode dell'id (0 per le entità senza id, come in JPABase).
   */
  @Override
  public int hashCode() {
    final Long key = identifier(this);
    return key == null ? 0 : key.hashCode();
  }

  /**
   * L'id dell'entità; per un proxy Hibernate è letto dal proxy stesso, senza caricare l'entità.
   */
  private static Long identifier(BaseModel model) {
    if (model instanceof HibernateProxy) {
      return (Long) ((HibernateProxy) model).getHibernateLazyInitializer().getIdentifier();
    }
    return model.getId();
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
