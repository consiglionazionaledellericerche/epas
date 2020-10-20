package cnr.sync.dto.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.PersonDay;
import org.modelmapper.ModelMapper;

/**
 * Dati esportati in Json per il PersonDay completi di persona.
 * 
 * @author cristian
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonDayShowDto extends PersonDayShowTerseDto {

  private PersonShowTerseDto person;

  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  /**
   * Nuova instanza di un PersonDayShowDto contenente i valori 
   * dell'oggetto personDay passato.
   */
  public static PersonDayShowDto build(PersonDay pd) {
    PersonDayShowDto personDto = 
        modelMapper.map(PersonDayShowTerseDto.build(pd), PersonDayShowDto.class);
    personDto.setPerson(PersonShowTerseDto.build(pd.person));
    return personDto;
  }
}
