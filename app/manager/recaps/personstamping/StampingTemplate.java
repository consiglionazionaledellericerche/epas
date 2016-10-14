package manager.recaps.personstamping;

import manager.cache.StampTypeManager;

import models.StampModificationType;
import models.StampModificationTypeCode;
import models.Stamping;

import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.List;

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

  List<StampModificationType> stampModificationTypes = Lists.newArrayList();

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

    //timbratura modificata dall'amministatore
    if (stamping.markedByAdmin) {
      stampModificationTypes.add(stampTypeManager.getStampMofificationType(
          StampModificationTypeCode.MARKED_BY_ADMIN));
    }

    //timbratura modificata dal dipendente
    if (stamping.markedByEmployee) {
      stampModificationTypes.add(stampTypeManager.getStampMofificationType(
          StampModificationTypeCode.MARKED_BY_EMPLOYEE));
    }

    //timbratura di mezzanotte
    if (stamping.stampModificationType != null) {
      stampModificationTypes.add(stamping.stampModificationType);
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

  /**
   * Se stampare il popover sulla stampingTemplate
   */
  public boolean showPopover() {
    if (!stampModificationTypes.isEmpty() || stamping.stampType != null) {
      return true;
    }
    return false;
  }

}
