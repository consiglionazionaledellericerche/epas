package manager.recaps.personStamping;

import manager.PersonDayManager;
import manager.cache.StampTypeManager;

import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;

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
  public String hour;
  public String markedByAdminCode;
  public String markedByEmployeeCode;
  public String identifier;
  public String missingExitStampBeforeMidnightCode;
  public boolean valid;

  private static final String STAMPING_FORMAT = "HH:mm";

  /**
   * Costruttore per la StampingTemplate.
   *
   * @param personDayManager injected
   * @param stampTypeManager injected
   * @param stamping         la timbratura formato BaseModel.
   * @param position         la posizione della timbratura all'interno della sua coppia [left,
   *                         center, right, none] (none per nessuna coppia)
   */
  public StampingTemplate(PersonDayManager personDayManager, StampTypeManager stampTypeManager,
                          Stamping stamping, String position) {

    this.stamping = stamping;
    this.stampingId = stamping.id;
    this.pairId = stamping.pairId;

    this.pairPosition = position;

    //stamping nulle o exiting now non sono visualizzate
    if (stamping.date == null || stamping.exitingNow) {
      this.hour = "";
      this.markedByAdminCode = "";
      this.markedByEmployeeCode = "";
      this.identifier = "";
      this.missingExitStampBeforeMidnightCode = "";
      this.valid = true;
      setColor(stamping);
      return;
    }

    this.date = stamping.date;

    this.way = stamping.way.getDescription();

    this.hour = stamping.date.toString(STAMPING_FORMAT);

    //timbratura di servizio
    this.identifier = "";
    if (stamping.stampType != null) {
      this.identifier = stamping.stampType.getIdentifier();
    }

    //timbratura modificata dall'amministatore
    this.markedByAdminCode = "";
    if (stamping.markedByAdmin) {
      StampModificationType smt = stampTypeManager.getStampMofificationType(
          StampModificationTypeCode.MARKED_BY_ADMIN);
      this.markedByAdminCode = smt.code;
    }

    //timbratura modificata dal dipendente
    this.markedByEmployeeCode = "";
    if (stamping.markedByEmployee != null && stamping.markedByEmployee) {
      StampModificationType smt = stampTypeManager.getStampMofificationType(
          StampModificationTypeCode.MARKED_BY_EMPLOYEE);
      this.markedByEmployeeCode = smt.code;
    }

    //timbratura di mezzanotte
    this.missingExitStampBeforeMidnightCode = "";
    if (stamping.stampModificationType != null
        && stamping.stampModificationType.code.equals(StampModificationTypeCode
        .TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getCode())) {

      this.missingExitStampBeforeMidnightCode = stamping
          .stampModificationType.code;
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

  protected void setHour(LocalDateTime date) {
    String hour = date.getHourOfDay() + "";
    if (hour.length() == 1)
      hour = "0" + hour;
    String minute = date.getMinuteOfHour() + "";
    if (minute.length() == 1)
      minute = "0" + minute;
    this.hour = hour + ":" + minute;
  }
}
