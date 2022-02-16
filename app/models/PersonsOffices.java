package models;

import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import lombok.extern.slf4j.Slf4j;
import models.base.PeriodModel;

@Slf4j
@Entity
@Audited
@Table(name = "persons_offices")
public class PersonsOffices extends PeriodModel{
  
  public Person person;
  public Office office;

}
