package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import models.BadgeReader;
import org.modelmapper.ModelMapper;

/**
 * Dati minimale per un BadgeReader (lettore badge) esportati in Json.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@ToString
@Data
@EqualsAndHashCode
public class BadgeReaderShowMinimalDto {
  
  private Long id;

  private String code;

  private String username;

  private boolean enabled;

  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;
  
  /**
   * Nuova instanza di un BadgeReaderShowTerseDto contenente i valori 
   * dell'oggetto badgeReader passato.
   */
  public static BadgeReaderShowMinimalDto buildMinimal(BadgeReader badgeReader) {
    val dto = modelMapper.map(badgeReader, BadgeReaderShowMinimalDto.class);
    dto.setUsername(badgeReader.user.username);
    return dto;
  }
}
