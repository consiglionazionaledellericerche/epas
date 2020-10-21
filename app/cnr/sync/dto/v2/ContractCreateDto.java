package cnr.sync.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import injection.StaticInject;
import java.time.LocalDate;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import models.Contract;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione via REST di un nuovo contratto di una persona.
 * 
 * @author cristian
 *
 */
@StaticInject
@Data
public class ContractCreateDto {
  
  @Required
  private Long personId;  
  @Required
  private LocalDate beginDate;
  private LocalDate endDate;
  private LocalDate endContract;
  private String perseoId;
  private Boolean onCertificate = Boolean.TRUE;

  //utilizzato per indicare alla classe ContractManager il tipo di
  //orario di lavoro da associare alla creazione del contratto.
  //Se il campo non è passato il contractManager associarà
  //il tipo di orario predefinito "Normale"
  private Long workingTimeTypeId;
  
  @JsonIgnore
  @Inject
  private static ModelMapper modelMapper;
  
  /**
   * Nuova istanza di un oggetto contract a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Contract build(ContractCreateDto contractDto) {
    val contract = modelMapper.map(contractDto, Contract.class);
    return contract;
  }
}