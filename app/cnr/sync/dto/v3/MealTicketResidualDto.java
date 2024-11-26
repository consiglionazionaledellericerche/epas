package cnr.sync.dto.v3;


import cnr.sync.dto.v2.PersonShowTerseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Person;
import org.joda.time.LocalDate;
import org.modelmapper.ModelMapper;

@ToString
@Data
@EqualsAndHashCode
public class MealTicketResidualDto {

  private Person person;
  private LocalDate dateOfResidual;
  private Long mealTicketResidual;
  
  public static MealTicketResidualDto build(PersonStampingRecap psDto) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    val mealTicketResidualDto = modelMapper.map(psDto, MealTicketResidualDto.class);
    if (psDto != null) {
      mealTicketResidualDto.setMealTicketResidual(psDto.contractMonths.stream()
          .mapToInt(cm -> cm.getValue().getRemainingMealTickets()).count());
      mealTicketResidualDto.setDateOfResidual(LocalDate.now());
    }
    return mealTicketResidualDto;
  }
}
