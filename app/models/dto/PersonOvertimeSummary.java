package models.dto;

import lombok.Getter;
import lombok.Setter;
import models.Person;

/**
 * Dto per ritornare nellapagina del monte ore della sede (per il responsabile di sede) o dell'assegnazione 
 * delle ore di straordinario (per il responsabile di gruppo) la situazione delle ore di straordinario
 * di ogni dipendente. 
 */
@Getter
@Setter
public class PersonOvertimeSummary {
  
  public Person person;
  public int assignedHours;
  public int remainingHours;

}
