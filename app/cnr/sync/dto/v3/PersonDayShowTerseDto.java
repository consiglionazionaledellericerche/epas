package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import injection.StaticInject;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.PersonDay;
import org.modelmapper.ModelMapper;

@StaticInject
@Data
@EqualsAndHashCode(of = "id")
public class PersonDayShowTerseDto {

  private Long id;
  private LocalDate date;
  private int timeAtWork;
  private int difference;
  private int progressive;
  private boolean isTicketAvailable;
  private boolean isHoliday;

  private List<StampingShowTerseDto> stampings = Lists.newArrayList();

  private List<AbsenceShowTerseDto> absences = Lists.newArrayList();
  
  @JsonIgnore
  @Inject
  static ModelMapper modelMapper;

  
  /**
   * Nuova instanza di un PersonDayShowTerseDto contenente i valori 
   * dell'oggetto personDay passato.
   */
  public static PersonDayShowTerseDto build(PersonDay pd) {
    val pdDto = modelMapper.map(pd, PersonDayShowTerseDto.class);

    pdDto.setAbsences(
        pd.absences.stream().map(a -> AbsenceShowTerseDto.build(a))
          .collect(Collectors.toList())); 
   
    pdDto.setStampings(
        pd.stampings.stream().map(s -> StampingShowTerseDto.build(s))
        .collect(Collectors.toList()));
    return pdDto;
  }
}
