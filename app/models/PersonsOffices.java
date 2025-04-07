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

import com.google.common.collect.Range;
import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;
import models.base.PropertyInPeriod;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;


/**
 * Classe di relazione tra la persona e l'ufficio periodicizzata.

 * @author dario
 *
 */
@Entity
@Audited
@Table(name = "persons_offices")
public class PersonsOffices extends PropertyInPeriod implements IPropertyInPeriod {
  
  @ManyToOne
  public Person person;
  
  @ManyToOne
  public Office office;
  
  /**
   * Metodo per la restituzione del periodo di afferenza di una persona ad una sede.

   * @return il range di date in cui una persona afferisce ad una sede.
   */
  public Range<LocalDate> dateRange() {
    if (getBeginDate() == null && getEndDate() == null) {
      return Range.all();
    }
    if (getBeginDate() == null) {
      return Range.atMost(getEndDate());
    }
    if (getEndDate() == null) {
      return Range.atLeast(getBeginDate());
    }
    return Range.closed(getBeginDate(), getEndDate());
  }


  @Override
  public IPropertiesInPeriodOwner getOwner() {
    
    return this.person;
  }


  @Override
  public void setOwner(IPropertiesInPeriodOwner target) {
    this.person = (Person) target;    
  }


  @Override
  public Object getType() {
    return this.getClass();
  }


  @Override
  public void setType(Object value) {
    //questo metodo in questo caso non serve, perchè i periods sono tutti dello stesso tipo.    
  }


  @Override
  public Object getValue() {
    return this.office;
  }


  @Override
  public void setValue(Object value) {
    this.office = (Office) value;    
  }


  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof PersonsOffices) {
      return this.getValue().equals(((PersonsOffices) otherValue));
    }
    return false;
  }

}
