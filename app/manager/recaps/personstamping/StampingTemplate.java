package manager.recaps.personstamping;

import manager.cache.StampTypeManager;

import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;
import models.enumerate.StampTypes;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * Oggetto che modella la singola timbratura nelle viste personStamping e stampings.
 *
 * @author alessandro
 */
public class StampingTemplate {

  public Stamping stamping;
  public Long stampingId;
  public String colour;
  public int pairId;
  public String pairPosition;            //left center right none
  public LocalDateTime date;
  
  public String way;
  public String hour = "";
  public StampModificationType markedByAdmin = null;
  public StampModificationType markedByEmployee = null;
  public StampModificationType missingExitStampBeforeMidnight = null;
  public StampTypes stampType = null;
  public boolean valid;

  private static final String STAMPING_FORMAT = "HH:mm";

  /**
   * Costruttore per la StampingTemplate.
   *
   * @param stampTypeManager injected
   * @param stamping         la timbratura formato BaseModel.
   * @param position         la posizione della timbratura all'interno della sua coppia [left,
   *                         center, right, none] (none per nessuna coppia)
   */
  public StampingTemplate(StampTypeManager stampTypeManager, Stamping stamping, String position) {

    this.stamping = stamping;
    this.stampingId = stamping.id;
    this.pairId = stamping.pairId;

    this.pairPosition = position;

    //stamping nulle o exiting now non sono visualizzate
    if (stamping.date == null || stamping.exitingNow) {
      this.valid = true;
      setColor(stamping);
      return;
    }

    this.date = stamping.date;

    this.way = stamping.way.getDescription();

    this.hour = stamping.date.toString(STAMPING_FORMAT);

    //timbratura di servizio
    if (stamping.stampType != null) {
      this.stampType = stamping.stampType;
    }

    //timbratura modificata dall'amministatore
    if (stamping.markedByAdmin) {
      StampModificationType smt = stampTypeManager.getStampMofificationType(
          StampModificationTypeCode.MARKED_BY_ADMIN);
      this.markedByAdmin = smt;
    }

    //timbratura modificata dal dipendente
    if (stamping.markedByEmployee != null && stamping.markedByEmployee) {
      StampModificationType smt = stampTypeManager.getStampMofificationType(
          StampModificationTypeCode.MARKED_BY_EMPLOYEE);
      this.markedByEmployee = smt;
    }

    //timbratura di mezzanotte
    if (stamping.stampModificationType != null
        && stamping.stampModificationType.code.equals(StampModificationTypeCode
        .TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getCode())) {
      this.missingExitStampBeforeMidnight = stamping.stampModificationType;
    }

    //timbratura valida (colore cella)
    LocalDate today = new LocalDate();
    LocalDate stampingDate = new LocalDate(this.date.getYear(),
        this.date.getMonthOfYear(), this.date.getDayOfMonth());
    if (today.isEqual(stampingDate)) {
      this.valid = true;
    } else {
      this.valid = stamping.isValid();
    }

    setColor(stamping);
  }

  protected void setColor(Stamping stamping) {
    this.colour = stamping.way.description;
    if (!this.valid) {
      this.colour = "warn";
    }
  }

}
