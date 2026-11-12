/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import javax.inject.Inject;
import lombok.Data;
import lombok.val;
import manager.StampingManager;
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.StampTypes;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;

/**
 * Dati per l'aggiornamento via REST di una timbratura di una persona.
 * Data e ora della timbratura non sono aggiornabili, eventualmente cancellare
 * e re-inserire la timbratura.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
public class StampingUpdateDto {

  //Causale timbratura motiviDiServizio, pausaPranzo, lavoroFuoriSede
  private StampTypeDto reasonType;

  // in / out 
  private WayType wayType;
  
  private String note;
  private String place;
  private String reason;

  /**
   * Ignorato: le timbrature create o aggiornate via REST dai lettori badge non risultano mai
   * inserite da un amministratore. Il campo è mantenuto per compatibilità con i client
   * esistenti.
   */
  private boolean markedByAdmin;

  /**
   * Ignorato: le timbrature create o aggiornate via REST dai lettori badge non risultano mai
   * inserite dal dipendente. Il campo è mantenuto per compatibilità con i client esistenti.
   */
  private boolean markedByEmployee;

  /**
   * questo nuovo campo si è reso necessario per la sede centrale per capire da quale lettore 
   * proviene la timbratura così da poter applicare un algoritmo che giustifichi le timbrature 
   * di uscita/ingresso consecutive dei dipendenti se provenienti da lettori diversi e appartenenti 
   * a un collegamento definito.e all'interno della tolleranza definita per quel collegamento.
   */
  public String stampingZone;

  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;

  /**
   * Nuova istanza di un oggetto Stamping a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Stamping build(StampingUpdateDto stampingDto) {
    val stamping = toStamping(stampingDto);

    if (stampingDto.reasonType != null) {
      stamping.setStampType(StampTypes.byCode(stampingDto.reasonType.name()));
    }

    return stamping;
  }

  /**
   * Nuova istanza di Stamping con i valori del DTO, esclusi markedByAdmin e markedByEmployee:
   * chi ha inserito la timbratura non è deciso dal client.
   */
  @SuppressWarnings("unchecked")
  static synchronized Stamping toStamping(StampingUpdateDto stampingDto) {
    val sourceType = (Class<StampingUpdateDto>) stampingDto.getClass();
    TypeMap<StampingUpdateDto, Stamping> typeMap =
        modelMapper.getTypeMap(sourceType, Stamping.class);
    if (typeMap == null) {
      typeMap = modelMapper.createTypeMap(sourceType, Stamping.class)
          .addMappings(mapper -> {
            mapper.skip(Stamping::setMarkedByAdmin);
            mapper.skip(Stamping::setMarkedByEmployee);
          });
    }
    return typeMap.map(stampingDto);
  }

  /**
   * Aggiorna i dati dell'oggetto Stamping passato con quelli
   * presenti nell'instanza di questo DTO. La timbratura modificata dal lettore badge non
   * risulta più inserita da un amministratore né dal dipendente: markedByAdmin e
   * markedByEmployee sono azzerati, qualunque valore invii il client.
   */
  public void update(Stamping stamping) {
    stamping.setMarkedByAdmin(false);
    stamping.setMarkedByEmployee(false);
    stamping.setNote(StampingManager.clientText(getNote()));
    stamping.setPlace(StampingManager.clientText(getPlace()));
    stamping.setReason(StampingManager.clientText(getReason()));
    stamping.setStampingZone(StampingManager.clientText(getStampingZone()));
    stamping.setWay(getWayType());
    if (getReasonType() != null) {
      stamping.setStampType(StampTypes.byCode(getReasonType().name()));
    } else {
      stamping.setStampType(null);
    }
  }
}