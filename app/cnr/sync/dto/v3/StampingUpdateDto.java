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
import models.Stamping;
import models.Stamping.WayType;
import models.enumerate.StampTypes;
import org.modelmapper.ModelMapper;

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
   * questo campo booleano consente di determinare se la timbratura è stata effettuata dall'utente
   * all'apposito lettore badge (valore = false) o se è stato l'amministratore a settare l'orario di
   * timbratura.
   */
  private boolean markedByAdmin;

  /**
   * Da impostare a true quando è il dipendente a modificare la propria timbratura.
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
    val stamping = modelMapper.map(stampingDto, Stamping.class);

    if (stampingDto.reasonType != null) {
      stamping.setStampType(StampTypes.byCode(stampingDto.reasonType.name()));
    }

    return stamping;
  }

  /**
   * Aggiorna i dati dell'oggetto Stamping passato con quelli
   * presenti nell'instanza di questo DTO.
   */
  public void update(Stamping stamping) {
    stamping.setMarkedByAdmin(isMarkedByAdmin());
    stamping.setMarkedByEmployee(isMarkedByEmployee());
    stamping.setNote(getNote());
    stamping.setPlace(getPlace());
    stamping.setReason(getReason());
    stamping.setStampingZone(getStampingZone());
    stamping.setWay(getWayType());
    if (getReasonType() != null) {
      stamping.setStampType(StampTypes.byCode(getReasonType().name()));
    }
  }
}