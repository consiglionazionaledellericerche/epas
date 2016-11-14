package models.exports;

import models.BadgeReader;
import models.Person;
import models.enumerate.StampTypes;

import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative ad una timbratura.
 *
 * @author cristian
 */
public class StampingFromClient {

  public String numeroBadge;
  public Integer inOut;
  public BadgeReader badgeReader;
  public StampTypes stampType;
  public Person person;
  public boolean markedByAdmin;
  public LocalDateTime dateTime;

}
