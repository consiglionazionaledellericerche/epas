package models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.persistence.Transient;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.enumerate.TeleworkStampTypes;
import org.joda.time.YearMonth;



@Data
@Builder()
public class TeleworkDto {
  
  private Long id;

  private long personDayId;
  
  private TeleworkStampTypes stampType;
  
  private LocalDateTime date;
    
  private String note;

  /**
   * Utile per effettuare i controlli temporali sulle drools.
   *
   * @return il mese relativo alla data della timbratura.
   */
  public YearMonth getYearMonth() {
    return new YearMonth(date.getYear(), date.getMonthValue());
  }
  
  public boolean isPersistent() {
    return id != null;
  }
  
  /**
   * Orario formattato come HH:mm.
   * @return orario della timbratura formattato come HH:mm.
   */
  @Transient
  public String formattedHour() {
    if (this.date != null) {
      return date.format(DateTimeFormatter.ofPattern("HH:mm"));
      //return date.toString("HH:mm");
    } else {
      return "";
    }
  }
  
  @Override
  public String toString() {
    return "Id timbratura: "+ id + ", PersonDayId: " + personDayId + ", Causale: " 
        + stampType + ", Data: " + date + ", Note: " + note;
  }
}
