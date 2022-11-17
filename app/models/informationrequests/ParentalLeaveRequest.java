package models.informationrequests;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.InformationRequest;
import org.hibernate.envers.Audited;
import play.data.validation.Required;
import play.db.jpa.Blob;

/**
 * Classe di richiesta di uscite di servizio.
 *
 * @author dario
 *
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "parental_leave_requests")
@PrimaryKeyJoinColumn(name = "informationRequestId")
public class ParentalLeaveRequest extends InformationRequest {

  private static final long serialVersionUID = -8903988853720152320L;
  
  @Required
  @NotNull
  private LocalDate beginDate;
  @Required
  @NotNull
  private LocalDate endDate;
  
  private Blob bornCertificate;
  
  private Blob expectedDateOfBirth;
}
