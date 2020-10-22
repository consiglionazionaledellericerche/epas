package cnr.sync.dto;

import com.google.common.base.Function;
import models.Person;

public class SimplePersonDto {
  
  public long id;
  public String firstname;
  public String surname;
  public String updatedAt;
  public String email;
  public String uidCnr;
  
  public enum FromPerson implements Function<Person, SimplePersonDto> {
    ISTANCE;
    
    @Override
    public SimplePersonDto apply(Person person) {
      SimplePersonDto personDto = new SimplePersonDto();
      personDto.id = person.id;
      personDto.firstname = person.name;
      personDto.surname = person.surname;
      personDto.email = person.email;
      personDto.uidCnr = person.eppn;
      return personDto;
    }
  }
}
