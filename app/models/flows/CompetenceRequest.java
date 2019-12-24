package models.flows;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import org.joda.time.LocalDateTime;
import models.Person;
import models.base.MutableModel;
import play.data.validation.Required;

public class CompetenceRequest extends MutableModel {

  @Required
  @NotNull
  @ManyToOne(optional = false)
  public Person person;
  
  /**
   * Descrizione della richiesta
   */
  public String note;
  
  /**
   * Data e ora di inizio.
   */
  @Required
  @NotNull
  @Column(name = "start_at")
  public LocalDateTime startAt;

  @Column(name = "end_to")
  public LocalDateTime endTo;
  
  public LocalDateTime employeeApproved;
  
  public LocalDateTime managerApproved;
  
  public LocalDateTime officeHeadApproved;
  
  public LocalDateTime administrativeApproved;
}
